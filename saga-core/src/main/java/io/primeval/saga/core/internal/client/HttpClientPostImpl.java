package io.primeval.saga.core.internal.client;


import io.primeval.saga.http.client.method.HttpClientPost;
import io.primeval.saga.http.protocol.HttpMethod;

public final class HttpClientPostImpl extends HttpClientBodyMethodImpl<HttpClientPost>implements HttpClientPost {

    public HttpClientPostImpl(BoundHttpClientImpl boundClient, String uri) {
        super(boundClient, HttpMethod.POST, uri);
    }

}
