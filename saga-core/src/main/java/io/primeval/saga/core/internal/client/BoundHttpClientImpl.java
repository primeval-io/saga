package io.primeval.saga.core.internal.client;

import io.primeval.saga.http.client.BoundHttpClient;
import io.primeval.saga.http.client.method.HttpClientDelete;
import io.primeval.saga.http.client.method.HttpClientGet;
import io.primeval.saga.http.client.method.HttpClientPost;
import io.primeval.saga.http.client.method.HttpClientPut;
import io.primeval.saga.http.protocol.HttpHost;

public final class BoundHttpClientImpl implements BoundHttpClient {

    final HttpClientInternal httpClient;
    final String host;
    final int port;
    final HttpHost destination;

    public BoundHttpClientImpl(HttpClientInternal httpClient, String host, int port) {
        this.httpClient = httpClient;
        this.host = host;
        this.port = port;
        this.destination = new HttpHost("http" + (httpClient.secure() ? "s" : ""), host, port);
    }

    @Override
    public HttpClientGet get(String uri) {
        return new HttpClientGetImpl(this, uri);
    }

    @Override
    public HttpClientPost post(String uri) {
        return new HttpClientPostImpl(this, uri);
    }

    @Override
    public HttpClientDelete delete(String uri) {
        return new HttpClientDeleteImpl(this, uri);
    }

    @Override
    public HttpClientPut put(String uri) {
        return new HttpClientPutImpl(this, uri);
    }

}
