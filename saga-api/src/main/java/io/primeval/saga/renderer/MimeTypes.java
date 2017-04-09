package io.primeval.saga.renderer;

public interface MimeTypes {
    /**
     * Content-Type of text.
     */
    public static final String TEXT = "text/plain";
    /**
     * Content-Type of html.
     */
    public static final String HTML = "text/html";
    /**
     * Content-Type of json.
     */
    public static final String JSON = "application/json";
    /**
     * Content-Type of xml.
     */
    public static final String XML = "application/xml";
    /**
     * Content-Type of css.
     */
    public static final String CSS = "text/css";
    /**
     * Content-Type of javascript.
     */
    public static final String JAVASCRIPT = "text/javascript";
    /**
     * Content-Type of form-urlencoded.
     */
    public static final String FORM = "application/x-www-form-urlencoded";
    /**
     * Content-Type of server sent events.
     */
    public static final String EVENT_STREAM = "text/event-stream";
    /**
     * Content-Type of binary data.
     */
    public static final String BINARY = "application/octet-stream";

    /**
     * Multipart.
     */
    public static final String MULTIPART = "multipart/form-data";
}
