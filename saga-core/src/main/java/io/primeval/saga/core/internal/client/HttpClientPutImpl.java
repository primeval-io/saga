package io.primeval.saga.core.internal.client;


import io.primeval.saga.http.client.method.HttpClientPut;
import io.primeval.saga.http.protocol.HttpMethod;

public final class HttpClientPutImpl extends HttpClientBodyMethodImpl<HttpClientPut>implements HttpClientPut {

    public HttpClientPutImpl(BoundHttpClientImpl boundClient, String uri) {
        super(boundClient, HttpMethod.PUT, uri);
    }

}
