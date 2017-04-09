package io.primeval.saga.ninio.internal.server;

import java.nio.ByteBuffer;

import com.davfx.ninio.http.HttpContentReceiver;

class IgnoreContentHttpContentReceiver implements HttpContentReceiver {
    public static final IgnoreContentHttpContentReceiver INSTANCE = new IgnoreContentHttpContentReceiver();

    private IgnoreContentHttpContentReceiver() {
    }

    @Override
    public void ended() {
    }

    @Override
    public void received(ByteBuffer buffer) {
    }
}