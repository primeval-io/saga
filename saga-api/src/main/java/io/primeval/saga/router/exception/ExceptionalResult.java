package io.primeval.saga.router.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

import io.primeval.saga.http.protocol.HttpRequest;

public final class ExceptionalResult {

    public final HttpRequest httpRequest;

    public final Throwable throwable;

    public ExceptionalResult(HttpRequest httpRequest, Throwable throwable) {
        this.httpRequest = httpRequest;
        this.throwable = throwable;
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return "An exception happened on " + httpRequest.method + " " + httpRequest.uri + ": " + sw.toString();
    }
}
