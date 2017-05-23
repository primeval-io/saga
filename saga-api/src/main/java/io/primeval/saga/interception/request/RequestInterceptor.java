package io.primeval.saga.interception.request;

import io.primeval.saga.router.RequestHandler;

@FunctionalInterface
public interface RequestInterceptor extends RequestHandler {

    default boolean matches(String uri) {
        return true;
    }

    default boolean applyRecovery() {
        return true;
    }
}
