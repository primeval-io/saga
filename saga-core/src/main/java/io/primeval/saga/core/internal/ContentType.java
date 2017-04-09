package io.primeval.saga.core.internal;

import java.util.Collections;
import java.util.Map;

import com.google.common.base.Joiner;

import io.primeval.saga.renderer.MimeTypes;

public final class ContentType {

    public final static ContentType UTF8_PLAIN_TEXT = new ContentType(MimeTypes.TEXT, Collections.singletonMap("charset", "utf-8"));

    public final String mediaType;
    public final Map<String, String> options;

    public ContentType(String contentType, Map<String, String> options) {
        super();
        this.mediaType = contentType;
        this.options = options;
    }

    public String repr() {
        return mediaType +  "; " + Joiner.on(" ; ").withKeyValueSeparator(" = ").join(options);
    }

}
