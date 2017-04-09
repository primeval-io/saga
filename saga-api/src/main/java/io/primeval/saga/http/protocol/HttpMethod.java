package io.primeval.saga.http.protocol;

/**
 * The HTTP Method.
 */
public enum HttpMethod {

    HEAD,

    GET,
    POST,
    PUT,
    DELETE,

    OPTIONS,
    PATCH;

    public static HttpMethod from(String method) {
        return HttpMethod.valueOf(method.toUpperCase());
    }

}
