package io.primeval.saga.http.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.primeval.saga.http.client.method.HttpClientDelete;
import io.primeval.saga.http.client.method.HttpClientGet;
import io.primeval.saga.http.client.method.HttpClientPost;
import io.primeval.saga.http.client.method.HttpClientPut;
import io.primeval.saga.http.escaper.UrlEscapers;

public interface BoundHttpClient {
    static final Pattern PATH_PATTERN = Pattern.compile("\\{+[^\\}]*\\}+");

    /**
     * Create a GET method.
     *
     * @param uriPath
     *            The uri path example : "/topology/networkElements/{tag}/attributes/{attribute}"
     * @param pathFragments
     *            The uri path fragments that will be replaced into the uriPath. The fragments are replaced in the path
     *            according to their order in the uriPath. Each fragment is escaped using
     *            UrlEscapers.urlPathSegmentEscaper().
     * @return The created Http method to make the call.
     */
    default HttpClientGet get(String uriPath, String... pathFragments) {
        return get(Collections.emptyMap(), uriPath, pathFragments);
    }

    /**
     * Create a GET method.
     *
     * @param uriParameters
     *            The query parameters to append to the uri of the form
     *            "/uri?param1=value1&param1=value2&param2=value2". (Support multiple values uri parameters.)
     * @param uriPath
     *            The uri path example : "/topology/networkElements/{tag}/attributes/{attribute}"
     * @param pathFragments
     *            The uri path fragments that will be replaced into the uriPath. The fragments are replaced in the path
     *            according to their order in the uriPath. Each fragment is escaped using
     *            UrlEscapers.urlPathSegmentEscaper().
     * @return The created Http method to make the call.
     */
    default HttpClientGet get(Map<String, List<String>> uriParameters, String uriPath, String... pathFragments) {
        String uri = buildUri(uriParameters, uriPath, pathFragments);
        return get(uri);
    }

    HttpClientGet get(String uri);

    /**
     * Create a POST method.
     *
     * @param uriPath
     *            The uri path example : "/topology/networkElements/{tag}/attributes/{attribute}"
     * @param pathFragments
     *            The uri path fragments that will be replaced into the uriPath. The fragments are replaced in the path
     *            according to their order in the uriPath. Each fragment is escaped using
     *            UrlEscapers.urlPathSegmentEscaper().
     * @return The created Http method to make the call.
     */
    default HttpClientPost post(String uriPath, String... pathFragments) {
        return post(Collections.emptyMap(), uriPath, pathFragments);
    }

    /**
     * Create a POST method.
     *
     * @param uriPath
     *            The uri path example : "/topology/networkElements/{tag}/attributes/{attribute}"
     * @param pathFragments
     *            The uri path fragments that will be replaced into the uriPath. The fragments are replaced in the path
     *            according to their order in the uriPath. Each fragment is escaped using
     *            UrlEscapers.urlPathSegmentEscaper().
     * @return The created Http method to make the call.
     */
    default HttpClientDelete delete(String uriPath, String... pathFragments) {
        return delete(Collections.emptyMap(), uriPath, pathFragments);
    }

    /**
     * Create a POST method.
     *
     * @param uriParameters
     *            The query parameters to append to the uri of the form
     *            "/uri?param1=value1&param1=value2&param2=value2". (Support multiple values uri parameters.)
     * @param uriPath
     *            The uri path example : "/topology/networkElements/{tag}/attributes/{attribute}"
     * @param pathFragments
     *            The uri path fragments that will be replaced into the uriPath. The fragments are replaced in the path
     *            according to their order in the uriPath. Each fragment is escaped using
     *            UrlEscapers.urlPathSegmentEscaper().
     * @return The created Http method to make the call.
     */
    default HttpClientPost post(Map<String, List<String>> uriParameters, String uriPath, String... pathFragments) {
        String uri = buildUri(uriParameters, uriPath, pathFragments);
        return post(uri);
    }

    HttpClientPost post(String uri);

    /**
     * Create a DELETE method.
     *
     * @param uriParameters
     *            The query parameters to append to the uri of the form
     *            "/uri?param1=value1&param1=value2&param2=value2". (Support multiple values uri parameters.)
     * @param uriPath
     *            The uri path example : "/topology/networkElements/{tag}/attributes/{attribute}"
     * @param pathFragments
     *            The uri path fragments that will be replaced into the uriPath. The fragments are replaced in the path
     *            according to their order in the uriPath. Each fragment is escaped using
     *            UrlEscapers.urlPathSegmentEscaper().
     * @return The created Http method to make the call.
     */
    default HttpClientDelete delete(Map<String, List<String>> uriParameters, String uriPath, String... pathFragments) {
        String uri = buildUri(uriParameters, uriPath, pathFragments);
        return delete(uri);
    }

    HttpClientDelete delete(String uri);

    /**
     * Create a PUT method.
     *
     * @param uriPath
     *            The uri path example : "/topology/networkElements/{tag}/attributes/{attribute}"
     * @param pathFragments
     *            The uri path fragments that will be replaced into the uriPath. The fragments are replaced in the path
     *            according to their order in the uriPath. Each fragment is escaped using
     *            UrlEscapers.urlPathSegmentEscaper().
     * @return The created Http method to make the call.
     */
    default HttpClientPut put(String uriPath, String... pathFragments) {
        return put(Collections.emptyMap(), uriPath, pathFragments);
    }

    /**
     * Create a PUT method.
     *
     * @param uriParameters
     *            The query parameters to append to the uri of the form
     *            "/uri?param1=value1&param1=value2&param2=value2". (Support multiple values uri parameters.)
     * @param uriPath
     *            The uri path example : "/topology/networkElements/{tag}/attributes/{attribute}"
     * @param pathFragments
     *            The uri path fragments that will be replaced into the uriPath. The fragments are replaced in the path
     *            according to their order in the uriPath. Each fragment is escaped using
     *            UrlEscapers.urlPathSegmentEscaper().
     * @return The created Http method to make the call.
     */
    default HttpClientPut put(Map<String, List<String>> uriParameters, String uriPath, String... pathFragments) {
        String uri = buildUri(uriParameters, uriPath, pathFragments);
        return put(uri);
    }

    HttpClientPut put(String uri);



    /**
     * Build the uri from the given uri parameters, uri path and path fragments.
     *
     * @param uriParameters
     *            The query parameters to append to the uri of the form
     *            "/uri?param1=value1&param1=value2&param2=value2". (Support multiple values uri parameters.)
     * @param uriPath
     *            The uri path example : "/topology/networkElements/{tag}/attributes/{attribute}"
     * @param pathFragments
     *            The uri path fragments that will be replaced into the uriPath. The fragments are replaced in the path
     *            according to their order in the uriPath. Each fragment is escaped using
     *            UrlEscapers.urlPathSegmentEscaper().
     * @return The resulting uri completed with parameters and fragments (correclty encoded).
     */
    default String buildUri(Map<String, List<String>> uriParameters, String uriPath, String[] pathFragments) {
        StringBuilder uri = new StringBuilder();
        // Append uri path with path fragments replaced
        for (String pathFragment : pathFragments) {
            Matcher matcher = PATH_PATTERN.matcher(uriPath);
            uriPath = matcher.replaceFirst(UrlEscapers.urlPathSegmentEscaper().escape(pathFragment));
        }
        uri.append(uriPath);
        // Append uri parameters
        boolean isFirst = true;
        for (Map.Entry<String, List<String>> entry : uriParameters.entrySet()) {
            String name = entry.getKey();
            List<String> value = entry.getValue();
            for (String v : value) {
                appendUriParameter(uri, isFirst, name, v);
                if (isFirst) {
                    isFirst = false;
                }
            }
        }

        return uri.toString().replaceAll(":", "%3A");
    }

    default void appendUriParameter(StringBuilder uri, boolean isFirst, String name, String value) {
        if (isFirst) {
            uri.append('?');
        } else {
            uri.append('&');
        }
        uri.append(name)
                .append('=')
                .append(UrlEscapers.urlPathSegmentEscaper().escape(value));
    }

}
