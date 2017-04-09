package io.primeval.saga.http.server.spi;

import io.primeval.saga.http.protocol.HttpRequest;
import io.primeval.saga.http.protocol.HttpResponse;
import io.primeval.saga.http.shared.Payload;

public interface HttpServerEvent {

    HttpRequest request();

    Payload content();

    void respond(HttpResponse response, Payload payload);

}
