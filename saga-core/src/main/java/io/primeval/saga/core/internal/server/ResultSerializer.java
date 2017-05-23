package io.primeval.saga.core.internal.server;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

import io.primeval.codex.promise.PromiseHelper;
import io.primeval.common.type.TypeTag;
import io.primeval.saga.action.ActionFunction;
import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;
import io.primeval.saga.core.internal.ContentType;
import io.primeval.saga.core.internal.SagaCoreUtils;
import io.primeval.saga.core.internal.http.shared.MimeParse;
import io.primeval.saga.guava.ImmutableResult;
import io.primeval.saga.http.protocol.HeaderNames;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.router.RequestHandler;
import io.primeval.saga.router.Route;
import io.primeval.saga.serdes.serializer.Serializable;
import io.primeval.saga.serdes.serializer.Serializer;

// Makes any Result<?> a Result<Payload>
public final class ResultSerializer implements RequestHandler {

    private final Serializer serializer;

    public ResultSerializer(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public Promise<Result<?>> onRequest(Context context, ActionFunction function, Optional<Route> boundRoute) {
        List<String> acceptHeaders = context.request().headers.get(HeaderNames.ACCEPT);
        String accept = acceptHeaders == null || acceptHeaders.isEmpty() ? "*/*" : acceptHeaders.get(0);

        return PromiseHelper.wrapPromise(() -> function.apply(context)).flatMap(result -> {
            return serializePayloadResult(result, accept);
        });
    }

    public <T> Promise<Result<?>> serializePayloadResult(Result<?> originResult, String accept) {
        // Finalized results always have a content (but it may be an empty payload)
        Result<?> result = finalizeResult(originResult);
        Serializable<?> serializable = result.content().get();

        TypeTag<?> resultType = serializable.typeTag();

        // Handle Payload types: no serialization/content-type discovery.
        if (resultType.rawType() == Payload.class) {
            return Promises.resolved(result);
        }

        Promise<ContentType> contentTypePms = SagaCoreUtils.determineContentType(result.headers())
                .map(Promises::resolved)
                .orElseGet(
                        () -> serializer.serializableMediaTypes(resultType)
                                .map(supportedMediaTypes -> new ContentType(
                                        MimeParse.bestMatch(supportedMediaTypes, accept),
                                        Collections.emptyMap())));
        return contentTypePms.flatMap(contentType -> {
            Promise<Payload> payloadPms = serializer.serialize(serializable, contentType.mediaType, contentType.options);

            return payloadPms.map(payload -> {
                ImmutableResult.Builder<Object> bldr = ImmutableResult.copySetupOf(result).setValue(payload);
                if (!result.headers().containsKey(HeaderNames.CONTENT_TYPE)) {
                    bldr.withHeader(HeaderNames.CONTENT_TYPE, contentType.repr());
                }
                return bldr.build();
            });
        });
    }

    private Result<?> finalizeResult(Result<?> result) {
        // Handle empty results

        if (!result.content().isPresent()) {
            return withEmptyPayload(result);
        } else {
            Serializable<?> s = result.content().get();
            if (s.typeTag().rawType() == Void.class) {
                return withEmptyPayload(result);
            }

            else {
                return result;
            }
        }
    }

    private ImmutableResult<Payload> withEmptyPayload(Result<?> result) {
        return ImmutableResult.<Payload> copySetupOf(result).setValue(Payload.empty()).build();
    }
}
