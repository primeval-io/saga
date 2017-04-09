package io.primeval.saga.core.internal.server;

import java.util.List;

import com.google.common.collect.ImmutableSortedMap;

import io.primeval.saga.http.protocol.HttpRequest;
import io.primeval.saga.http.protocol.HttpResponse;
import io.primeval.saga.http.server.spi.HttpServerEvent;
import io.primeval.saga.http.shared.Payload;

public final class HttpServerEventImpl implements HttpServerEvent {

    private final HttpServerEvent backendEvent;
    private final HttpRequest request;

    public HttpServerEventImpl(HttpServerEvent backendEvent) {
        this.backendEvent = backendEvent;
        HttpRequest backendRequest = backendEvent.request();
        this.request = new HttpRequest(backendRequest.host, backendRequest.method,
                backendRequest.uri, backendRequest.path, backendRequest.parameters,
                ImmutableSortedMap.<String, List<String>> orderedBy(String.CASE_INSENSITIVE_ORDER)
                        .putAll(backendRequest.headers).build());
    }

    @Override
    public HttpRequest request() {
        return request;
    }

    @Override
    public Payload content() {
        return backendEvent.content();
    }

    @Override
    public void respond(HttpResponse response, Payload payload) {
        backendEvent.respond(response, payload);
    }

}
