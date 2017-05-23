package io.primeval.saga.core.internal.server;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;

import org.osgi.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.primeval.codex.dispatcher.Dispatcher;
import io.primeval.codex.promise.PromiseHelper;
import io.primeval.common.type.TypeTag;
import io.primeval.saga.action.Action;
import io.primeval.saga.action.ActionFunction;
import io.primeval.saga.action.ActionKey;
import io.primeval.saga.action.Result;
import io.primeval.saga.core.internal.ContentType;
import io.primeval.saga.core.internal.SagaCoreUtils;
import io.primeval.saga.core.internal.action.ContextImpl;
import io.primeval.saga.http.protocol.HeaderNames;
import io.primeval.saga.http.protocol.HttpRequest;
import io.primeval.saga.http.protocol.HttpResponse;
import io.primeval.saga.http.protocol.Status;
import io.primeval.saga.http.server.spi.HttpServerEvent;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.interception.request.RequestInterceptor;
import io.primeval.saga.parameter.HttpParameterConverter;
import io.primeval.saga.renderer.MimeTypes;
import io.primeval.saga.router.Route;
import io.primeval.saga.router.Router;
import io.primeval.saga.router.RouterAction;
import io.primeval.saga.router.exception.ExceptionRecoveryHandler;
import io.primeval.saga.serdes.deserializer.Deserializer;
import io.primeval.saga.serdes.serializer.Serializer;

public final class HttpServerEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerEventHandler.class);

    private final Dispatcher dispatcher;
    private final Router router;
    private final Serializer serializer;
    private final Deserializer deserializer;
    private final HttpParameterConverter paramConverter;

    private final ExceptionRecoveryHandler exceptionRecoveryHandler;
    private final Supplier<Collection<RequestInterceptor>> requestInterceptors;
    private final Supplier<ImmutableSet<String>> excludeFromCompression;

    private final ResultSerializer payloadSerializer;

    public HttpServerEventHandler(Dispatcher dispatcher, Router router,
            Supplier<Collection<RequestInterceptor>> requestInterceptors, ExceptionRecoveryHandler exceptionRecoveryHandler,
            Serializer serializer,
            Deserializer deserializer,
            HttpParameterConverter paramConverter, Supplier<ImmutableSet<String>> excludeFromCompression) {
        this.dispatcher = dispatcher;
        this.router = router;
        this.requestInterceptors = requestInterceptors;
        this.exceptionRecoveryHandler = exceptionRecoveryHandler;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.paramConverter = paramConverter;
        this.excludeFromCompression = excludeFromCompression;
        this.payloadSerializer = new ResultSerializer(serializer);
    }

    // TODO cache the pipeline?
    public Promise<Action> getPipelineAction(HttpRequest request) {

        Promise<Optional<RouterAction>> routerActionPms = router.getActionFor(request.method.name(), request.path);

        return routerActionPms.map(routerAction -> {
            Optional<Route> boundRoute = routerAction.map(ra -> ra.route);

            Collection<RequestInterceptor> interceptors = requestInterceptors.get();

            // Interceptors in reverse order ; they are wrapped so leaving them
            // in that order enables them in the "right" order!
            List<RequestInterceptor> activeInterceptors = interceptors.stream()
                    .filter(f -> f.matches(request.uri)).collect(Collectors.toList());

            ActionKey actionKey;
            ActionFunction fun;

            if (routerAction.isPresent()) {
                Action ra = routerAction.get().action;
                actionKey = ra.actionKey;
                fun = wrapInDispatcher(ra.function);
            } else {
                Action ra = notFoundAction();
                actionKey = ra.actionKey;
                fun = ra.function;
            }

            for (RequestInterceptor f : activeInterceptors) {
                ActionFunction nextFun = f.applyRecovery() ? exceptionRecoveryHandler.wrap(fun, boundRoute) : fun;
                fun = f.wrap(nextFun, boundRoute);
            }

            // fix any failed result and serialize.
            fun = payloadSerializer.wrap(exceptionRecoveryHandler.wrap(fun, boundRoute), boundRoute);
            
            // if serialization fails, we must recover, and serialize the recovery
            fun = payloadSerializer.wrap(exceptionRecoveryHandler.wrap(fun, boundRoute), boundRoute);

            // if it fails again, forget it.
            
            return new Action(actionKey, fun);
        });

    }

    public void onEvent(HttpServerEvent event) {

        HttpRequest request = event.request();

        Promise<Action> pipelineActionPms = getPipelineAction(request);

        Promise<PayloadResult> payloadResPms = pipelineActionPms.flatMap(pipelineAction -> {

            ContextImpl actionContext = new ContextImpl(event, deserializer, paramConverter);

            Promise<Result<?>> promise = pipelineAction.function.apply(actionContext);

            return promise.map(result -> {
                // At this stage successful results have to be present payloads, the pipeline takes care of it.
                Payload payload = Payload.class.cast(result.content().get().value());
                return new PayloadResult(result.statusCode(), payload, result.headers());
            });
        }).recoverWith(p -> PromiseHelper.recoverFromWith(p, Throwable.class, error -> {
            return basicErrorResponse(request, error);
        }));

        PromiseHelper.onResolve(payloadResPms, payloadRes -> {

            Payload payload = payloadRes.payload;
            Map<String, List<String>> headers = payloadRes.headers;
            if (payload.contentLength.isPresent()) {
                long cl = payload.contentLength.getAsLong();
                String mimeType = SagaCoreUtils.determineContentType(payloadRes.headers).map(ct -> ct.mediaType)
                        .orElse(MimeTypes.BINARY);

                if (excludeFromCompression.get().contains(mimeType)) {

                    Iterable<Map.Entry<String, List<String>>> it = payloadRes.headers.entrySet().stream()
                            .filter(e -> !e.getKey().equals(HeaderNames.CONTENT_LENGTH))::iterator;
                    headers = ImmutableMap.<String, List<String>> builder()
                            .putAll(it)
                            .put(HeaderNames.CONTENT_LENGTH, Collections.singletonList(String.valueOf(cl))).build();
                }
            }
            HttpResponse response = new HttpResponse(payloadRes.status, "", headers);
            event.respond(response, payload);

        }, failure -> {
            LOGGER.error("Could not send result", failure);
        });

    }

    private Promise<PayloadResult> basicErrorResponse(HttpRequest request, Throwable error) {
        Map<String, List<String>> headers = Multimaps
                .asMap(ImmutableListMultimap.of(HeaderNames.CONTENT_TYPE, MimeTypes.TEXT + "; charset = utf-8"));
        ContentType contentType = SagaCoreUtils.determineContentType(headers).orElse(ContentType.UTF8_PLAIN_TEXT);

        LOGGER.error("An exception was not recovered on {} {}", request.method, request.uri, error);
        Promise<Payload> payloadPms = serializer.serialize("Something wrong happened\n " + error.getMessage(),
                TypeTag.of(String.class),
                contentType.mediaType, contentType.options);
        return payloadPms.map(payload -> new PayloadResult(Status.INTERNAL_SERVER_ERROR, payload, headers));
    }

    private Action notFoundAction() {
        return DefaultActions.NOT_FOUND; // could be overriden by service
    }

    private ActionFunction wrapInDispatcher(ActionFunction fun) {
        return context -> dispatcher.dispatch(() -> fun.apply(context)).flatMap(x -> x);
    }

}
