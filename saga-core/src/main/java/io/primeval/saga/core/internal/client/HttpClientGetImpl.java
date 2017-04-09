package io.primeval.saga.core.internal.client;


import io.primeval.saga.http.client.method.HttpClientGet;
import io.primeval.saga.http.protocol.HttpMethod;

public final class HttpClientGetImpl extends HttpClientMethodImpl<HttpClientGet>implements HttpClientGet {

    public HttpClientGetImpl(BoundHttpClientImpl boundClient, String uri) {
        super(boundClient, HttpMethod.GET, uri);
    }
}
