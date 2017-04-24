package io.primeval.saga.serdes.serializer;

import java.util.Map;
import java.util.Set;

import org.osgi.util.promise.Promise;

import io.primeval.common.type.TypeTag;
import io.primeval.saga.http.shared.Payload;

public interface Serializer {

    <T> Promise<Payload> serialize(T object, TypeTag<? extends T> typeTag, String mediaType,
            Map<String, String> options);

    Promise<Boolean> canSerialize(TypeTag<?> typeTag, String mediaType);

    Promise<Set<String>> serializableMediaTypes(TypeTag<?> typeTag);

    Promise<Set<String>> serializableMediaTypes();

    default <T> Promise<Payload> serialize(Serializable<T> serializable, String mediaType,
            Map<String, String> options) {
        return serialize(serializable.value(), serializable.typeTag(), mediaType, options);
    }

}
