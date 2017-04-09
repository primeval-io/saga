package io.primeval.saga.http.client;

public interface HttpClient {

    HttpClient secure(boolean secure);

    HttpClient as(String userAgent);

    // Default
    HttpClient withExecutionContext();

    HttpClient withoutExecutionContext();

    // Default: none(!)
    HttpClient withContentType(String mimeType);

    BoundHttpClient to(String host, int port);

}
