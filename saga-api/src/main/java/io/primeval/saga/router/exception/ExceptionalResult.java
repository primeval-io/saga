package io.primeval.saga.router.exception;

import io.primeval.saga.http.protocol.HttpMethod;
import io.primeval.saga.http.protocol.HttpRequest;

public final class ExceptionalResult {

    public final HttpRequest httpRequest;

    public final Throwable throwable;

    public ExceptionalResult(HttpRequest httpRequest, Throwable throwable) {
        this.httpRequest = httpRequest;
        this.throwable = throwable;
    }

}
