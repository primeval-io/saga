package io.primeval.saga.http.client;

/**
 * Checked exception on purpose
 */
public class HttpClientException extends RuntimeException {

    public final String url;

    public final int code;

    public HttpClientException(String url, int httpCode) {
        this.url = url;
        this.code = httpCode;
    }

    @Override
    public String getMessage() {
        return "Error contacting url '" + url + "' : " + code + ' ' + super.getMessage();
    }
}
