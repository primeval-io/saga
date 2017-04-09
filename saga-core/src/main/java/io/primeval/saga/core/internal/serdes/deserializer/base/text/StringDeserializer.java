package io.primeval.saga.core.internal.serdes.deserializer.base.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.osgi.util.promise.Promise;
import org.reactivestreams.Publisher;

import io.primeval.codex.promise.PromiseHelper;
import io.primeval.common.bytebuffer.ByteBufferListInputStream;
import io.primeval.common.type.TypeTag;
import io.primeval.saga.http.shared.Payload;
import io.primeval.common.serdes.DeserializationException;
import io.primeval.saga.serdes.deserializer.spi.TypeDeserializer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class StringDeserializer implements TypeDeserializer<String> {

    @Override
    public Promise<String> deserialize(Payload payload, String mimeType, Map<String, String> options) {

        String charset = options.getOrDefault("charset", "utf-8");
        return deserializeString(payload.content, mimeType, charset);
    }

    private Promise<String> deserializeString(Publisher<ByteBuffer> buffer, String mimeType, String charset) {
        // new ByteBufferListInputStream(
        Mono<List<ByteBuffer>> buffersM = Flux.from(buffer).collectList();

        return PromiseHelper.fromMono(buffersM).map(buffers -> {
            ByteBufferListInputStream bufferListInputStream = new ByteBufferListInputStream(buffers);
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(bufferListInputStream, charset))) {
                return bufferedReader.lines().collect(Collectors.joining("\n"));
            } catch (IOException e) {
                throw new DeserializationException(TypeTag.of(String.class), mimeType, e);
            }
        });
    }
}
