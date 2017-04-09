package io.primeval.saga.core.internal;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import io.primeval.saga.http.protocol.HeaderNames;

public final class SagaCoreUtils {

    private SagaCoreUtils() {
    }

    public static final Optional<ContentType> determineContentType(Map<String, List<String>> headers) {
        List<String> contentTypeResp = headers.get(HeaderNames.CONTENT_TYPE);
        String contentType = null;
        if (contentTypeResp != null && contentTypeResp.size() == 1) {
            Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

            String[] ctParts = contentTypeResp.get(0).split(";");
            contentType = ctParts[0];

            for (int i = 0; i < ctParts.length; i++) {

                String opt = ctParts[i];
                int eqPos = opt.indexOf('=');
                if (eqPos != -1) {
                    String k = opt.substring(0, eqPos).trim();
                    String v = opt.substring(eqPos + 1).trim();
                    options.put(k, v);
                }
            }
            return Optional.of(new ContentType(contentType, options));

        }
        return Optional.empty();
    }

}
