package io.primeval.saga.core.internal.server;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;

import io.primeval.codex.dispatcher.Dispatcher;
import io.primeval.codex.promise.PromiseHelper;
import io.primeval.common.type.TypeTag;
import io.primeval.saga.action.Action;
import io.primeval.saga.action.Result;
import io.primeval.saga.core.internal.ContentType;
import io.primeval.saga.core.internal.SagaCoreUtils;
import io.primeval.saga.core.internal.action.ContextImpl;
import io.primeval.saga.core.internal.http.shared.MimeParse;
import io.primeval.saga.http.protocol.HeaderNames;
import io.primeval.saga.http.protocol.HttpRequest;
import io.primeval.saga.http.protocol.HttpResponse;
import io.primeval.saga.http.protocol.Status;
import io.primeval.saga.http.server.spi.HttpServerEvent;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.parameter.HttpParameterConverter;
import io.primeval.saga.renderer.MimeTypes;
import io.primeval.saga.router.Router;
import io.primeval.saga.router.RouterAction;
import io.primeval.saga.router.filter.RouteFilterProvider;
import io.primeval.saga.serdes.deserializer.Deserializer;
import io.primeval.saga.serdes.serializer.Serializer;

public final class HttpServerEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerEventHandler.class);

    private final Dispatcher dispatcher;
    private final Router router;
    private final Serializer serializer;
    private final Deserializer deserializer;
    private final HttpParameterConverter paramConverter;

    private final AtomicReference<SortedMap<Orderer<RouteFilterProvider>, RouteFilterProvider>> routeFilterProviders;

    public HttpServerEventHandler(Dispatcher dispatcher, Router router,
            AtomicReference<SortedMap<Orderer<RouteFilterProvider>, RouteFilterProvider>> routeFilterProviders,
            Serializer serializer,
            Deserializer deserializer,
            HttpParameterConverter paramConverter) {
        this.dispatcher = dispatcher;
        this.router = router;
        this.routeFilterProviders = routeFilterProviders;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.paramConverter = paramConverter;
    }

    public Promise<Action> getAction(HttpServerEvent event) {

        HttpRequest request = event.request();
        List<String> path = event.request().path;

        Promise<Optional<RouterAction>> routerActionPms = router.getActionFor(request.method.name(), path);

        Collection<RouteFilterProvider> filters = routeFilterProviders.get().values();

        List<RouteFilterProvider> activeFilters = Lists.reverse(filters.stream()
                .filter(f -> f.matches(request.uri)).collect(Collectors.toList()));

        return routerActionPms.map(routerAction -> {
            Action nextAction = routerAction.map(ba -> wrapInDispatcher(ba.action)).orElse(notFoundAction());
            for (RouteFilterProvider f : activeFilters) {
                // TODO can filters change type?
                Action next = nextAction;
                Action filterAction = new Action(context -> {
                    return PromiseHelper.wrapPromise(() -> f.call(context, next, routerAction.map(ra -> ra.route)));
                }, nextAction.actionType, nextAction.actionKey);
                nextAction = filterAction;
            }
            return nextAction;
        });

    }

    public void onEvent(HttpServerEvent event) {

        HttpRequest request = event.request();

        List<String> acceptHeaders = request.headers.get(HeaderNames.ACCEPT);
        String accept = acceptHeaders == null || acceptHeaders.isEmpty() ? "*/*" : acceptHeaders.get(0);

        System.out.println(accept);

        Promise<Action> actionForRoute = getAction(event);

        ContextImpl actionContext = new ContextImpl(event, deserializer, paramConverter);

        @SuppressWarnings("unchecked")
        Promise<PayloadResult> payloadResPms = actionForRoute.flatMap(action -> {
            Promise<Result<?>> promise = action.function.apply(actionContext);

            return promise
                    .flatMap(result -> {

                        TypeTag resultType = action.actionType; // allow redef

                        if (resultType.rawType() == Payload.class) {
                            Payload payload = (Payload) result.payload;
                            return Promises.resolved(new PayloadResult(result.statusCode, payload, result.headers));
                        }

                        Promise<ContentType> contentTypePms = SagaCoreUtils.determineContentType(result.headers)
                                .map(Promises::resolved)
                                .orElseGet(
                                        () -> serializer.serializableMediaTypes(resultType)
                                                .map(supportedMediaTypes -> new ContentType(
                                                        MimeParse.bestMatch(supportedMediaTypes, accept),
                                                        Collections.emptyMap())));
                        return contentTypePms
                                .flatMap(contentType -> {
                                    Promise<Payload> payloadPms = serializer.serialize(result.payload,
                                            resultType, contentType.mediaType, contentType.options);

                                    Result<?> r = result.headers.containsKey(HeaderNames.CONTENT_TYPE) ? result
                                            : result.withHeader(HeaderNames.CONTENT_TYPE, contentType.repr());

                                    return payloadPms
                                            .map(payload -> new PayloadResult(r.statusCode, payload, r.headers));
                                });
                    });
        })/* chain in more recovery */.recoverWith(p -> PromiseHelper.recoverFromWith(p, Throwable.class, error -> {
            Map<String, List<String>> headers = Multimaps
                    .asMap(ImmutableListMultimap.of(HeaderNames.CONTENT_TYPE, MimeTypes.TEXT + "; charset = utf-8"));
            ContentType contentType = SagaCoreUtils.determineContentType(headers).orElse(ContentType.UTF8_PLAIN_TEXT);

            error.printStackTrace();
            Promise<Payload> payloadPms = serializer.serialize("Something wrong happened\n " + error.getMessage(),
                    TypeTag.of(String.class),
                    contentType.mediaType, contentType.options);
            return payloadPms.map(payload -> new PayloadResult(Status.INTERNAL_SERVER_ERROR, payload, headers));
        }));

        PromiseHelper.onResolve(payloadResPms, payloadRes -> {

            Payload payload = payloadRes.payload;
            HttpResponse response = new HttpResponse(payloadRes.status, "", payloadRes.headers);

            event.respond(response, payload);

            // GC/Flux subscription thing to explore
            // Flux.from(respondPublisher).doOnError(error -> {
            // LOGGER.trace("Could not send full payload (maybe client disconnected)", error);
            // }).subscribe();

        }, failure -> {
            LOGGER.error("Could not send result", failure);
        });

    }

    private Action notFoundAction() {
        return DefaultActions.NOT_FOUND; // could be overriden by service
    }

    private Action wrapInDispatcher(Action a) {
        return new Action(context -> dispatcher.dispatch(() -> a.function.apply(context)).flatMap(x -> x),
                a.actionType, a.actionKey);

    }

}
