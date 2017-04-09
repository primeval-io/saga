package io.primeval.saga.core.internal.serdes.serializer.base.text;

import java.nio.ByteBuffer;
import java.util.Map;

import org.osgi.util.promise.Promise;

import io.primeval.codex.promise.PromiseHelper;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.serdes.serializer.spi.TypeSerializer;
import reactor.core.publisher.Mono;

public final class ObjectToTextSerializer implements TypeSerializer<Object> {

    @Override
    public Promise<Payload> serialize(Object object, String mimeType, Map<String, String> options) {

        String charset = options.getOrDefault("charset", "utf-8");
        return serializeString(object.toString(), charset);
    }

    private Promise<Payload> serializeString(String object, String charset) {
        return PromiseHelper.wrap(() -> {
            byte[] b = object.toString().getBytes(charset);
            return Payload.ofLength(b.length, Mono.just(ByteBuffer.wrap(b)));
        });
    }
}
