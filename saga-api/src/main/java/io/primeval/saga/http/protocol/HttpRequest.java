package io.primeval.saga.http.protocol;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class HttpRequest {

    public final HttpHost host;
    public final HttpMethod method;
    public final String uri;
    public final List<String> path;
    public final Map<String, List<Optional<String>>> parameters;
    public final Map<String, List<String>> headers;

    public HttpRequest(HttpHost host, HttpMethod method, String uri, List<String> path, Map<String, List<Optional<String>>> parameters,
            Map<String, List<String>> headers) {
        super();
        this.host = host;
        this.method = method;
        this.uri = uri;
        this.path = path;
        this.parameters = parameters;
        this.headers = headers;
    }
    
}