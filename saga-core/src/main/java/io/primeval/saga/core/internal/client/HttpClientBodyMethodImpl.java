package io.primeval.saga.core.internal.client;

import java.util.Optional;

import io.primeval.common.type.TypeTag;
import io.primeval.saga.http.client.method.HttpClientBodyMethod;
import io.primeval.saga.http.protocol.HttpMethod;

@SuppressWarnings("unchecked")
public abstract class HttpClientBodyMethodImpl<T extends HttpClientBodyMethod<T>> extends HttpClientMethodImpl<T>
        implements HttpClientBodyMethod<T> {

    private Body<?> body;

    public HttpClientBodyMethodImpl(BoundHttpClientImpl boundClient, HttpMethod method, String uri) {
        super(boundClient, method, uri);
    }

    @Override
    public <I> T withBody(I body, TypeTag<? extends I> type) {
        this.body = new Body<I>(body, type, null);
        return (T) this;
    }

    @Override
    public <I> T withBody(I body, TypeTag<? extends I> type, String mimeType) {
        this.body = new Body<I>(body, type, mimeType);
        return (T) this;
    }

    @Override
    <I> Optional<Body<I>> body() {
        return Optional.ofNullable((Body<I>) body);
    }

}
