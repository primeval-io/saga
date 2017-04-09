package io.primeval.saga.http.client;

import java.util.List;
import java.util.Map;

import io.primeval.saga.http.shared.Payload;

public final class HttpClientRawResponse extends HttpClientResponse {
    public Payload payload;

    HttpClientRawResponse(int code, Map<String, List<String>> headers, Payload payload) {
        super(code, headers);
        this.payload = payload;
        
    }

}
