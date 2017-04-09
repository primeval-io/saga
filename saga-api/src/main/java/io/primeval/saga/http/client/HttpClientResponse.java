package io.primeval.saga.http.client;

import java.util.List;
import java.util.Map;

import io.primeval.saga.http.shared.Payload;

public abstract class HttpClientResponse {

    public final int code;
    public final Map<String, List<String>> headers;

    public HttpClientResponse(int code, Map<String, List<String>> headers) {
        super();
        this.code = code;
        this.headers = headers;
    }

    public static HttpClientRawResponse raw(int code, Map<String, List<String>> headers, Payload payLoad) {
        return new HttpClientRawResponse(code, headers, payLoad);
    }

    public static <T> HttpClientObjectResponse<T> object(int code, Map<String, List<String>> headers, T object) {
        return new HttpClientObjectResponse<>(code, headers, object);
    }

}
