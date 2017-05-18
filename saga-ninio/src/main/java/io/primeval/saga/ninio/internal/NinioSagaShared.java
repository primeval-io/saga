package io.primeval.saga.ninio.internal;

import java.util.List;
import java.util.Map;

import com.davfx.ninio.http.HttpRequest;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimaps;

import io.primeval.saga.http.protocol.HttpHost;
import io.primeval.saga.http.shared.Payload;

public final class NinioSagaShared {

    public NinioSagaShared() {
    }

    public static final String PROVIDER_NAME = "ninio";

    public static io.primeval.saga.http.protocol.HttpRequest toSagaRequest(HttpRequest req) {

        Map<String, List<java.util.Optional<String>>> queryParameters = Multimaps.asMap(Multimaps
                .transformValues(ensureListMultimap(HttpRequest.parameters(req.path)),
                        v -> java.util.Optional.ofNullable(v.orNull())));

        return new io.primeval.saga.http.protocol.HttpRequest(
                new HttpHost(req.address.secure ? "https" : "http", req.address.host, req.address.port),
                toSagaMethod(req.method), req.path, HttpRequest.path(req.path),
                queryParameters, toSagaHeaders(req.headers));
    }

    public static com.davfx.ninio.http.HttpMethod fromSagaMethod(io.primeval.saga.http.protocol.HttpMethod method) {
        switch (method) {
        case GET:
            return com.davfx.ninio.http.HttpMethod.GET;
        case POST:
            return com.davfx.ninio.http.HttpMethod.POST;
        case PUT:
            return com.davfx.ninio.http.HttpMethod.PUT;
        case DELETE:
            return com.davfx.ninio.http.HttpMethod.DELETE;
        case HEAD:
            return com.davfx.ninio.http.HttpMethod.HEAD;
        case OPTIONS:
            return com.davfx.ninio.http.HttpMethod.OPTIONS;
        default:
            throw new UnsupportedOperationException("Unsupported HTTP method: " + method.name());
        }
    }

    public static io.primeval.saga.http.protocol.HttpMethod toSagaMethod(com.davfx.ninio.http.HttpMethod method) {
        switch (method) {
        case GET:
            return io.primeval.saga.http.protocol.HttpMethod.GET;
        case POST:
            return io.primeval.saga.http.protocol.HttpMethod.POST;
        case PUT:
            return io.primeval.saga.http.protocol.HttpMethod.PUT;
        case DELETE:
            return io.primeval.saga.http.protocol.HttpMethod.DELETE;
        case HEAD:
            return io.primeval.saga.http.protocol.HttpMethod.HEAD;
        case OPTIONS:
            return io.primeval.saga.http.protocol.HttpMethod.OPTIONS;
        default:
            throw new UnsupportedOperationException("Unsupported HTTP method: " + method.name());
        }
    }

    public static final ImmutableListMultimap<String, String> fromSagaHeaders(Map<String, List<String>> headers,
            Payload payload) {
        ImmutableListMultimap.Builder<String, String> headerBldr = ImmutableListMultimap.builder();
        headers.entrySet().forEach(e -> headerBldr.putAll(e.getKey(), e.getValue()));
        ImmutableListMultimap<String, String> ninioHeaders = headerBldr.build();
        return ninioHeaders;
    }

    // need fix in Ninio API.
    public static <K, V> ImmutableListMultimap<K, V> ensureListMultimap(ImmutableMultimap<K, V> map) {
        if (map instanceof ImmutableListMultimap) {
            ImmutableListMultimap<K, V> listMultimap = (ImmutableListMultimap<K, V>) map;
            return listMultimap;
        }
        throw new AssertionError("should be ImmutableListMultimap");
    }

    public static final Map<String, List<String>> toSagaHeaders(ImmutableMultimap<String, String> headers) {
        return toSagaHeaders(ensureListMultimap(headers));
    }

    public static final Map<String, List<String>> toSagaHeaders(ImmutableListMultimap<String, String> headers) {
        return Multimaps.asMap(headers);
    }

    public static final io.primeval.saga.http.protocol.HttpResponse toSagaResponse(
            com.davfx.ninio.http.HttpResponse response) {
        return new io.primeval.saga.http.protocol.HttpResponse(response.status, response.reason,
                toSagaHeaders(response.headers));
    }

    public static final com.davfx.ninio.http.HttpResponse fromSagaResponse(
            io.primeval.saga.http.protocol.HttpResponse response,
            Payload payload) {
        ImmutableListMultimap<String, String> headers = fromSagaHeaders(response.headers, payload);
        return new com.davfx.ninio.http.HttpResponse(response.status, response.reason, headers);
    }

}
