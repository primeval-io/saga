package io.primeval.saga.core.internal.serdes.deserializer;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import io.primeval.codex.promise.PromiseCollector;
import io.primeval.codex.promise.PromiseHelper;
import io.primeval.common.property.PropertyHelper;
import io.primeval.common.serdes.DeserializationException;
import io.primeval.common.type.TypeTag;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.serdes.deserializer.Deserializer;
import io.primeval.saga.serdes.deserializer.SerDesConstants;
import io.primeval.saga.serdes.deserializer.spi.MediaDeserializer;
import io.primeval.saga.serdes.deserializer.spi.TypeDeserializer;

@Component(immediate = true)
public final class DeserializerImpl implements Deserializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeserializerImpl.class);

    private final Map<String, MediaDeserializer> mediaDeserializers = new ConcurrentHashMap<>();

    @Override
    public <T> Promise<T> deserialize(Payload payload, TypeTag<? extends T> typeTag, ClassLoader classLoader, String mediaType,
            Map<String, String> options) {
        return deserialization(typeTag, classLoader, mediaType).flatMap(des -> des.deserialize(payload, mediaType, options));
    }

    @Override
    public Promise<Boolean> canDeserialize(TypeTag<?> typeTag, ClassLoader classLoader, String mediaType) {
        return deserialization(typeTag, classLoader, mediaType).map(x -> true).recover(p -> false);
    }

    public <T> Promise<TypeDeserializer<T>> deserialization(TypeTag<? extends T> typeTag, ClassLoader classLoader,
            String mediaType) {
        return PromiseHelper.wrapPromise(e -> new DeserializationException(typeTag, mediaType, e),
                () -> {
                    MediaDeserializer mediaDeserializer = mediaDeserializers.get(mediaType);
                    if (mediaDeserializer == null) {
                        throw new NoSuchElementException("no deserializer");
                    }
                    return mediaDeserializer.typeDeserializer(typeTag, classLoader);
                });
    }

    public Promise<Set<String>> deserializableMediaTypes(TypeTag<?> typeTag, ClassLoader classLoader) {
        return mediaDeserializers.entrySet().stream()
                .map(mtd -> mtd.getValue().typeDeserializer(typeTag, classLoader)
                        .map(x -> mtd.getKey()))
                .collect(PromiseCollector.allSuccessful()).map(ImmutableSet::copyOf);
    }

    @Override
    public Promise<Set<String>> deserializableMediaTypes() {
        return PromiseHelper.wrap(() -> ImmutableSet.copyOf(mediaDeserializers.keySet()));
    }

    public void addMimeTypeDeserializer(String mediaType, MediaDeserializer mediaDeserializer) {
        this.mediaDeserializers.put(mediaType, mediaDeserializer);
    }

    public void removeMimeTypeDeserializer(String mediaType, MediaDeserializer mediaDeserializer) {
        this.mediaDeserializers.remove(mediaType, mediaDeserializer);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addMimeTypeDeserializer(MediaDeserializer mediaDeserializer, Map<String, Object> properties) {
        String[] mediaTypes = PropertyHelper.getProperty(SerDesConstants.MEDIATYPE_PROPERTY, String.class, properties);
        for (String mediaType : mediaTypes) {
            addMimeTypeDeserializer(mediaType, mediaDeserializer);
        }
    }

    public void removeMimeTypeDeserializer(MediaDeserializer mediaDeserializer, Map<String, Object> properties) {
        String[] mediaTypes = PropertyHelper.getProperty(SerDesConstants.MEDIATYPE_PROPERTY, String.class, properties);
        for (String mediaType : mediaTypes) {
            this.mediaDeserializers.remove(mediaType, mediaDeserializer);
        }
    }
}
