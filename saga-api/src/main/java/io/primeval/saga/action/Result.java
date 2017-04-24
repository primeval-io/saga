package io.primeval.saga.action;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.primeval.common.type.TypeTag;
import io.primeval.saga.http.protocol.Status;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.serdes.serializer.Serializable;

public interface Result<T> {

    int statusCode();

    Map<String, List<String>> headers();

    Serializable<T> contents();

    // Basic builders for headerless results 
    public static <T> Result<T> create(int status, T contents, TypeTag<T> explicitType) {
        return new EmptyHeadersResult<T>(status, Serializable.of(contents, explicitType));
    }

    public static <T> Result<T> create(int status, T contents) {
        return new EmptyHeadersResult<T>(status, Serializable.of(contents));
    }

    public static <T> Result<T> ok(T contents, TypeTag<T> explicitType) {
        return create(Status.OK, contents, explicitType);
    }

    public static <T> Result<T> ok(T contents) {
        return create(Status.OK, contents);
    }

    public static Result<Payload> ok() {
        return ok(Payload.EMPTY, Payload.TYPETAG);
    }

    public static <T> Result<T> badRequest(T contents, TypeTag<T> explicitType) {
        return create(Status.BAD_REQUEST, contents, explicitType);
    }

    public static <T> Result<T> badRequest(T contents) {
        return create(Status.BAD_REQUEST, contents);
    }

    public static Result<Payload> badRequest() {
        return badRequest(Payload.EMPTY, Payload.TYPETAG);
    }

}

/* package*/ final class EmptyHeadersResult<T> implements Result<T> {

    public final int statusCode;
    public final Serializable<T> contents;

    public EmptyHeadersResult(int statusCode, Serializable<T> contents) {
        this.statusCode = statusCode;
        this.contents = contents;
    }

    @Override
    public int statusCode() {
        return statusCode;
    }

    @Override
    public Map<String, List<String>> headers() {
        return Collections.emptyMap();
    }

    @Override
    public Serializable<T> contents() {
        return contents;
    }

}
