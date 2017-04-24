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
    public final Serializable<T> contents;

    private ImmutableResult(int statusCode, ImmutableListMultimap<String, String> headers, Serializable<T> contents) {
        super();
        this.statusCode = statusCode;
        this.headers = headers;
        this.contents = contents;
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
    public Serializable<T> contents() {
        return contents;
    }

    public static <T> Builder<T> copyOf(Result<T> result) {
        ArrayListMultimap<String, String> headers = ArrayListMultimap.create();
        result.headers().forEach((k, v) -> headers.putAll(k, v));
        Builder<T> builder = new Builder<T>().setValue(result.contents().value()).withStatusCode(result.statusCode())
                .withHeaders(headers);
        result.contents().explicitTypeTag().ifPresent(t -> builder.withExplicitType(t));
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
            Serializable<T> contents = explicitType.map(t -> Serializable.of(value, t))
                    .orElseGet(() -> Serializable.of(value));
            return new ImmutableResult<T>(statusCode, ImmutableListMultimap.copyOf(headers), contents);
        }

    }

}
