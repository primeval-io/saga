package io.primeval.saga.http.shared;

import java.nio.ByteBuffer;
import java.util.OptionalLong;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Mono;

public final class Payload {
    public static final Payload EMPTY = new Payload(OptionalLong.of(0), Mono.empty());

    public final OptionalLong contentLength;
    public final Publisher<ByteBuffer> content;

    private Payload(OptionalLong contentLength, Publisher<ByteBuffer> content) {
        this.contentLength = contentLength;
        this.content = content;
    }

    public static Payload stream(Publisher<ByteBuffer> content) {
        return new Payload(OptionalLong.empty(), content);
    }

    public static Payload ofLength(long length, Publisher<ByteBuffer> content) {
        return new Payload(OptionalLong.of(length), content);
    }

    public static Payload empty() {
        return EMPTY;
    }
}
