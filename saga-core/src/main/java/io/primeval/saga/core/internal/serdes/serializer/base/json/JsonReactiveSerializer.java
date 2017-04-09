package io.primeval.saga.core.internal.serdes.serializer.base.json;

import java.nio.ByteBuffer;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

import io.primeval.codex.promise.PromiseHelper;
import io.primeval.common.type.TypeTag;
import io.primeval.json.JsonSerializer;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.renderer.MimeTypes;
import io.primeval.saga.serdes.SupportsMediaTypes;
import io.primeval.saga.serdes.serializer.spi.MediaSerializer;
import io.primeval.saga.serdes.serializer.spi.TypeSerializer;
import reactor.core.publisher.Mono;

@Component
@SupportsMediaTypes(MimeTypes.JSON)
public final class JsonReactiveSerializer implements MediaSerializer {

    private JsonSerializer jsonSerializer;

    final class JsonAnyTypeSerializer<T> implements TypeSerializer<T> {

        private final TypeTag<? extends T> typeTag;

        public JsonAnyTypeSerializer(TypeTag<? extends T> typeTag) {
            this.typeTag = typeTag;
        }

        @Override
        public Promise<Payload> serialize(T object, String mediaType, Map<String, String> options) {
            return PromiseHelper.wrap(() -> {
                byte[] b = jsonSerializer.toByteArray(object, typeTag);
                return Payload.ofLength(b.length, Mono.just(ByteBuffer.wrap(b)));
            });
        }

    }

    @Override
    public <T> Promise<TypeSerializer<T>> typeSerializer(TypeTag<? extends T> typeTag) {
        return Promises.resolved(new JsonAnyTypeSerializer<>(typeTag));

    }

    @Reference
    public void setJsonSerializer(JsonSerializer jsonSerializer) {
        this.jsonSerializer = jsonSerializer;

    }

}
