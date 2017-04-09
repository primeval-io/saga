package io.primeval.saga.http.server.spi;

import org.osgi.util.promise.Promise;
import org.reactivestreams.Publisher;

public interface HttpServerProvider {

    Promise<Void> stop();

    Promise<Void> start(int port);

    Publisher<HttpServerEvent> eventStream();

    Promise<Integer> port();

}
