package io.primeval.saga.core.internal.server;

import java.util.List;
import java.util.Map;

import io.primeval.saga.http.shared.Payload;

public final class PayloadResult {

    public final int status;
    public final Payload payload;
    public final Map<String, List<String>> headers;

    public PayloadResult(int status, Payload payload, Map<String, List<String>> headers) {
        this.status = status;
        this.payload = payload;
        this.headers = headers;
    }

}
