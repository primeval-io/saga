package io.primeval.saga.action;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.primeval.common.type.TypeTag;
import io.primeval.saga.http.protocol.Status;
import io.primeval.saga.http.shared.Payload;

public interface Result<T> {

    int statusCode();

    Map<String, List<String>> headers();

    T contents();

    Optional<TypeTag<T>> explicitType();
    
    
    // Simple, basic builders
    
    public static <T> Result<T> create(int status, T contents, TypeTag<T> explicitType) {
        return new SimpleResult<T>(status, contents, Optional.of(explicitType));
    }

    public static <T> Result<T> create(int status, T contents) {
        return new SimpleResult<T>(status, contents, Optional.empty());
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
