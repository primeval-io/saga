package io.primeval.saga.http.protocol;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class HttpResponse {

    public final int status;
    public final String reason;
    public final Map<String, List<String>> headers;

    public HttpResponse(int status, String reason, Map<String, List<String>> headers) {
        this.status = status;
        this.reason = reason;
        this.headers = headers;
    }

    public HttpResponse(int status, String reason) {
        this(status, reason, Collections.emptyMap());
    }

    @Override
    public String toString() {
        return "[status=" + status + ", reason=" + reason + ", headers=" + headers + "]";
    }
}