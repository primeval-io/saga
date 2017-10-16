package io.primeval.saga.core.internal.serdes.deserializer.base.json;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;
import org.reactivestreams.Publisher;

import io.primeval.codex.promise.PromiseHelper;
import io.primeval.common.bytebuffer.ByteBufferListInputStream;
import io.primeval.common.type.GenericBoxes;
import io.primeval.common.type.TypeTag;
import io.primeval.json.JsonDeserializer;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.renderer.MimeTypes;
import io.primeval.saga.serdes.SupportsMediaTypes;
import io.primeval.saga.serdes.deserializer.spi.MediaDeserializer;
import io.primeval.saga.serdes.deserializer.spi.TypeDeserializer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@SupportsMediaTypes(MimeTypes.JSON)
public final class JsonReactiveDeserializer implements MediaDeserializer {

    private JsonDeserializer jsonDeserializer;

    final class JsonAnyTypeDeserializer<T> implements TypeDeserializer<T> {

        private final TypeTag<? extends T> typeTag;
        private final ClassLoader classLoader;

        public JsonAnyTypeDeserializer(TypeTag<? extends T> typeTag, ClassLoader classLoader) {
            this.typeTag = typeTag;
            this.classLoader = classLoader;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Promise<T> deserialize(Payload payload, String mimeType, Map<String, String> options) {
            Publisher<ByteBuffer> buffer = payload.content;
            Class<? extends T> destType = typeTag.rawType();

            // Move those to other bundles for class-space resolution?
            if (Mono.class.isAssignableFrom(destType)) {
                Mono<List<ByteBuffer>> mono = Flux.from(buffer).collectList();

                TypeTag<Object> monoType = GenericBoxes.typeParameter(typeTag);

                Mono<Object> res = mono.map(l -> fromJson(l, monoType, classLoader));

                return Promises.resolved((T) res);

            } else if (Publisher.class.isAssignableFrom(destType)) {
                throw new UnsupportedOperationException("Publisher deserialization not supported yet");
            }

            Mono<List<ByteBuffer>> mono = Flux.from(buffer).collectList();
            Mono<T> res = mono.flatMap(l -> fromJson(l, typeTag, classLoader));

            return PromiseHelper.fromMono(res);
        }

        <U> Mono<U> fromJson(List<ByteBuffer> fromBuffers, TypeTag<U> toType, ClassLoader classLoader) {
            if (fromBuffers.isEmpty()) {
                return Mono.empty();
            }

            InputStream inputStream = new ByteBufferListInputStream(fromBuffers);

            return Mono.justOrEmpty(jsonDeserializer.fromJson(inputStream, toType, classLoader));
        }
    }

    @Override
    public <T> Promise<TypeDeserializer<T>> typeDeserializer(TypeTag<? extends T> typeTag, ClassLoader classLoader) {
        return Promises.resolved(new JsonAnyTypeDeserializer<>(typeTag, classLoader));
    }

    @Reference
    public void setJsonDeserializer(JsonDeserializer jsonDeserializer) {
        this.jsonDeserializer = jsonDeserializer;

    }

}
