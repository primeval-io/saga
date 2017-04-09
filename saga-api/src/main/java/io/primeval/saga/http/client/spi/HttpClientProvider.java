package io.primeval.saga.http.client.spi;

import java.util.List;
import java.util.Map;

import org.osgi.util.promise.Promise;

import io.primeval.saga.http.client.HttpClientRawResponse;
import io.primeval.saga.http.protocol.HttpHost;
import io.primeval.saga.http.protocol.HttpMethod;
import io.primeval.saga.http.shared.Payload;

public interface HttpClientProvider {

    Promise<HttpClientRawResponse> sendRequest(HttpHost destination, HttpMethod method, String uri,
            Map<String, List<String>> headers, Payload payload);

}
