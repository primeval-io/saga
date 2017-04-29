package io.primeval.saga.action;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.primeval.common.type.TypeTag;
import io.primeval.saga.http.protocol.Status;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.serdes.serializer.Serializable;

public interface Result<T> {

    int statusCode();

    Map<String, List<String>> headers();

    Optional<Serializable<T>> content();

    @Override
    String toString();

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();

    public static <T> int hashCode(Result<T> result) {
        return Objects.hash(result.headers(), result.content()) * 31 + result.statusCode();
    }

    public static boolean equals(/* @NotNull */ Result<?> r1, /* @NotNull */ Result<?> r2) {
        if (r1 == r2)
            return true;

        return r1.statusCode() == r2.statusCode() && Objects.equals(r1.content(), r2.content())
                && Objects.equals(r1.headers(), r2.headers());
    }

    // Basic builders for headerless results
    public static <T> Result<T> create(int status, T contents, TypeTag<T> explicitType) {
        return new EmptyHeadersResult<T>(status, Serializable.of(contents, explicitType));
    }

    public static <T> Result<T> create(int status, T contents) {
        return new EmptyHeadersResult<T>(status, Serializable.of(contents));
    }

    public static <T> Result<T> create(int status) {
        return new EmptyHeadersResult<T>(status);
    }

    public static <T> Result<T> ok(T contents, TypeTag<T> explicitType) {
        return create(Status.OK, contents, explicitType);
    }

    public static <T> Result<T> ok(T contents) {
        return create(Status.OK, contents);
    }

    public static Result<Payload> ok() {
        return create(Status.OK);
    }

    public static <T> Result<T> notFound(T contents, TypeTag<T> explicitType) {
        return create(Status.NOT_FOUND, contents, explicitType);
    }

    public static <T> Result<T> notFound(T contents) {
        return create(Status.NOT_FOUND, contents);
    }

    public static Result<Payload> notFound() {
        return create(Status.NOT_FOUND);
    }

    public static <T> Result<T> badRequest(T contents, TypeTag<T> explicitType) {
        return create(Status.BAD_REQUEST, contents, explicitType);
    }

    public static <T> Result<T> badRequest(T contents) {
        return create(Status.BAD_REQUEST, contents);
    }

    public static Result<Payload> badRequest() {
        return create(Status.BAD_REQUEST);
    }

}

/* package */ final class EmptyHeadersResult<T> implements Result<T> {

    public final int statusCode;
    public final Serializable<T> content;

    public EmptyHeadersResult(int statusCode, Serializable<T> contents) {
        this.statusCode = statusCode;
        this.content = contents;
    }

    public EmptyHeadersResult(int statusCode) {
        this(statusCode, null);
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
    public Optional<Serializable<T>> content() {
        return Optional.ofNullable(content);
    }
    
    @Override
    public String toString() {
        return "Result{status=" + statusCode() + ", headers={}, content=" + content() + "}";
    }

    @Override
    public int hashCode() {
        return Result.hashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (!(obj instanceof Result<?>))
            return false;
        Result<?> that = (Result<?>) obj;
        return Result.equals(this, that);
    }

}
