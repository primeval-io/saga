package io.primeval.saga.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.primeval.saga.http.protocol.HeaderNames;
import io.primeval.saga.http.protocol.Status;

public final class Result<T> {

    public final int statusCode;

    public final Map<String, List<String>> headers;

    public final T payload;

    public Result(int statusCode, Map<String, List<String>> headers, T payload) {
        super();
        this.statusCode = statusCode;
        this.headers = headers;
        this.payload = payload;
    }

    public static <T> Result<T> ok(T payload) {
        return new Result<T>(Status.OK, Collections.emptyMap(), payload);
    }

    public static <T> Result<T> notFound(T payload) {
        return new Result<T>(Status.NOT_FOUND, Collections.emptyMap(), payload);

    }

    public Result<T> withHeader(String key, String value) {
        Map<String, List<String>> newHeaders = new LinkedHashMap<>(headers);
        newHeaders.compute(key, (k, v) -> {
            if (v == null) {
                return new ArrayList<>();
            } else {
                return new ArrayList<>(v);
            }
        }).add(value);
        return new Result<T>(statusCode, newHeaders, payload);

    }

    public Result<T> contentType(String mimeType) {
        return withHeader(HeaderNames.CONTENT_TYPE, mimeType + "; charset= utf-8");
    }

}
