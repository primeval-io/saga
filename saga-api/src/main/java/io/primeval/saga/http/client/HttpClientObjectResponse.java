package io.primeval.saga.http.client;

import java.util.List;
import java.util.Map;

/**
 * @author Simon Chemouil
 *
 * @param <T>
 */
public final class HttpClientObjectResponse<T> extends HttpClientResponse {

    public final T object;

    HttpClientObjectResponse(int code, Map<String, List<String>> headers, T object) {
        super(code, headers);
        this.object = object;
    }

    @Override
    public String toString() {
        return HttpClientObjectResponse.class.getSimpleName() + "{code=" +  this.code + ",headers=" + this.headers + "}";
    }

}
