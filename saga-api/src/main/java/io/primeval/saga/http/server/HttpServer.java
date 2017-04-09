package io.primeval.saga.http.server;

import org.osgi.util.promise.Promise;

public interface HttpServer {

    Promise<Void> start(int port);

    Promise<Void> stop();

    Promise<Integer> port();

}
