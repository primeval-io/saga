package io.primeval.saga.core.internal.server.websocket;

import io.primeval.saga.http.shared.Payload;

public final class WebSocketFrame {

    public static enum Type {
        TEXT, BINARY
    }

    public final Payload payload;
    public final Type type;

    public WebSocketFrame(Payload payload, Type type) {
        super();
        this.payload = payload;
        this.type = type;
    }

    public static WebSocketFrame binary(Payload payload) {
        return new WebSocketFrame(payload, Type.BINARY);
    }

    public static WebSocketFrame text(Payload payload) {
        return new WebSocketFrame(payload, Type.TEXT);
    }
}
