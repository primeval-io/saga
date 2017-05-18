package io.primeval.saga.guava;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;

import io.primeval.common.type.TypeTag;
import io.primeval.saga.action.Result;
import io.primeval.saga.http.protocol.HeaderNames;
import io.primeval.saga.http.protocol.Status;
import io.primeval.saga.serdes.serializer.Serializable;

public class ImmutableResult<T> implements Result<T> {

    public final int statusCode;
    public final ImmutableListMultimap<String, String> headers;
    public final Serializable<T> content;

    private ImmutableResult(int statusCode, ImmutableListMultimap<String, String> headers, Serializable<T> content) {
        super();
        this.statusCode = statusCode;
        this.headers = headers;
        this.content = content;
    }

    @Override
    public int statusCode() {
        return statusCode;
    }

    @Override
    public Map<String, List<String>> headers() {
        return Multimaps.asMap(headers);
    }

    @Override
    public Optional<Serializable<T>> content() {
        return Optional.ofNullable(content);
    }

    @Override
    public String toString() {
        return "ImmutableResult{status=" + statusCode() + ", headers=" + headers() + ", content=" + content() + "}";
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

    public static <T> Builder<T> copySetupAndContentOf(Result<T> result) {
        Builder<T> builder = copySetupOf(result);
        result.content().ifPresent(content -> {
            builder.setValue(content.value());
            content.explicitTypeTag().ifPresent(t -> builder.withExplicitType(t));
        });
        return builder;
    }

    public static <T> Builder<T> copySetupOf(Result<?> result) {
        ArrayListMultimap<String, String> headers = ArrayListMultimap.create();
        result.headers().forEach((k, v) -> headers.putAll(k, v));
        Builder<T> builder = new Builder<T>().withStatusCode(result.statusCode())
                .withHeaders(headers);
        return builder;
    }

    public static <T> Builder<T> builder() {
        return new Builder<T>();
    }

    public static <T> Builder<T> builder(T contents) {
        return new Builder<T>().setValue(contents);
    }

    public static <T> Builder<T> ok() {
        return new Builder<T>().withStatusCode(Status.OK);
    }

    public static <T> Builder<T> ok(T contents) {
        return new Builder<T>().withStatusCode(Status.OK).setValue(contents);
    }

    public static <T> ImmutableResult.Builder<T> notFound() {
        return new Builder<T>().withStatusCode(Status.NOT_FOUND);
    }

    public static <T> ImmutableResult.Builder<T> notFound(T contents) {
        return new Builder<T>().withStatusCode(Status.NOT_FOUND).setValue(contents);
    }

    public final static class Builder<T> {

        private int statusCode = Status.OK;
        private ListMultimap<String, String> headers = ArrayListMultimap.create();
        private boolean hasContent = false;
        private T value;
        private Optional<TypeTag<? extends T>> explicitType = Optional.empty();

        public Builder<T> withStatusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder<T> withHeaders(ListMultimap<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder<T> setValue(T value) {
            this.hasContent = true;
            this.value = value;
            return this;
        }

        public Builder<T> contentType(String contentType) {
            this.headers.put(HeaderNames.CONTENT_TYPE, contentType);
            return this;
        }

        public Builder<T> withHeader(String headerName, String value) {
            this.headers.put(headerName, value);
            return this;
        }

        public Builder<T> withExplicitType(TypeTag<? extends T> typeTag) {
            this.explicitType = Optional.of(typeTag);
            return this;
        }

        public ImmutableResult<T> build() {
            Serializable<T> contents = hasContent ? explicitType.map(t -> Serializable.of(value, t))
                    .orElseGet(() -> Serializable.of(value)) : null;
            return new ImmutableResult<T>(statusCode, ImmutableListMultimap.copyOf(headers), contents);
        }

    }

}
