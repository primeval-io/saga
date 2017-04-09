package io.primeval.saga.ninio.internal;

import java.nio.ByteBuffer;

import org.reactivestreams.Publisher;

import com.davfx.ninio.http.HttpContentReceiver;

import reactor.core.publisher.BlockingSink;
import reactor.core.publisher.UnicastProcessor;

public final class ContentReceiver implements HttpContentReceiver {

    private final BlockingSink<ByteBuffer> sink;
    private final UnicastProcessor<ByteBuffer> emitter;

    public ContentReceiver() {
        emitter = UnicastProcessor.create();
        this.sink = emitter.connectSink();
    }

    @Override
    public void ended() {
        sink.complete();
    }

    @Override
    public void received(ByteBuffer bb) {
        sink.next(bb);
    }

    public Publisher<ByteBuffer> asPublisher() {
        return emitter;
    }

}
