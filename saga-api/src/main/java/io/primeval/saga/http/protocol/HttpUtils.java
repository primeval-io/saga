package io.primeval.saga.http.protocol;

import java.util.List;
import java.util.Optional;

public final class HttpUtils {

    public static final Optional<String> getHeader(HttpRequest request, String headerName) {
        List<String> hs = request.headers.get(headerName);
        if (hs == null || hs.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(hs.get(0));
    }
}
