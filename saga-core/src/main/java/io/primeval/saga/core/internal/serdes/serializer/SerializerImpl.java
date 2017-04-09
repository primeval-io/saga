package io.primeval.saga.core.internal.serdes.serializer;

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
import io.primeval.common.type.TypeTag;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.serdes.deserializer.SerDesConstants;
import io.primeval.common.serdes.SerializationException;
import io.primeval.saga.serdes.serializer.Serializer;
import io.primeval.saga.serdes.serializer.spi.MediaSerializer;
import io.primeval.saga.serdes.serializer.spi.TypeSerializer;

@Component(immediate = true)
public final class SerializerImpl implements Serializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerializerImpl.class);

    private final Map<String, MediaSerializer> mediaSerializers = new ConcurrentHashMap<>();

    @Override
    public <T> Promise<Payload> serialize(T object, TypeTag<? extends T> typeTag, String mediaType, Map<String, String> options) {
        Promise<TypeSerializer<T>> serialization = serialization(typeTag, mediaType);
        return serialization.flatMap(ser -> ser.serialize(object, mediaType, options));
    }

    @Override
    public Promise<Boolean> canSerialize(TypeTag<?> typeTag, String mediaType) {
        return serialization(typeTag, mediaType).map(x -> true).recover(x -> false);
    }

    public <T> Promise<TypeSerializer<T>> serialization(TypeTag<? extends T> typeTag, String mediaType) {
        return PromiseHelper.wrapPromise(e -> new SerializationException(typeTag, mediaType, e),
                () -> {
                    MediaSerializer mediaSerializer = mediaSerializers.get(mediaType);
                    if (mediaSerializer == null) {
                        throw new NoSuchElementException("no serializer");
                    }
                    return mediaSerializer.typeSerializer(typeTag);
                });
    }

    @Override
    public Promise<Set<String>> serializableMediaTypes(TypeTag<?> typeTag) {
        return mediaSerializers.entrySet().stream()
                .map(mtd -> mtd.getValue().typeSerializer(typeTag)
                        .map(x -> mtd.getKey()))
                .collect(PromiseCollector.allSuccessful()).map(ImmutableSet::copyOf);
    }

    @Override
    public Promise<Set<String>> serializableMediaTypes() {
        return PromiseHelper.wrap(() -> ImmutableSet.copyOf(mediaSerializers.keySet()));
    }

    public void addMimeTypeSerializer(String mimeType, MediaSerializer mimeTypeSerializer) {
        this.mediaSerializers.put(mimeType, mimeTypeSerializer);
    }

    public void removeMimeTypeSerializer(String mimeType, MediaSerializer mimeTypeSerializer) {
        this.mediaSerializers.remove(mimeType, mimeTypeSerializer);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addMimeTypeSerializer(MediaSerializer mimeTypeSerializer, Map<String, Object> properties) {
        String[] mimeTypes = PropertyHelper.getProperty(SerDesConstants.MEDIATYPE_PROPERTY, String.class, properties);
        for (String mimeType : mimeTypes) {
            addMimeTypeSerializer(mimeType, mimeTypeSerializer);
        }
    }

    public void removeMimeTypeSerializer(MediaSerializer mimeTypeSerializer, Map<String, Object> properties) {
        String[] mimeTypes = PropertyHelper.getProperty(SerDesConstants.MEDIATYPE_PROPERTY, String.class, properties);
        for (String mimeType : mimeTypes) {
            this.mediaSerializers.remove(mimeType, mimeTypeSerializer);
        }
    }
}
