package io.primeval.saga.annotations.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class RouteUtils {

    public static List<String> path(String uri) {

        if (uri.isEmpty() || uri.equals("/")) {
            return Collections.emptyList();
        }

        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }

        return Arrays.asList(uri.split("\\/"));

    }

}
