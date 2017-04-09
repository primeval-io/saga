package io.primeval.saga.core.internal.client;


import io.primeval.saga.http.client.method.HttpClientDelete;
import io.primeval.saga.http.protocol.HttpMethod;

public final class HttpClientDeleteImpl extends HttpClientMethodImpl<HttpClientDelete>implements HttpClientDelete {

    public HttpClientDeleteImpl(BoundHttpClientImpl boundClient, String uri) {
        super(boundClient, HttpMethod.DELETE, uri);
    }

}
