package io.primeval.saga.serdes.deserializer;

import java.util.Map;
import java.util.Set;

import org.osgi.util.promise.Promise;

import io.primeval.common.type.TypeTag;
import io.primeval.saga.http.shared.Payload;

public interface Deserializer {

    <T> Promise<T> deserialize(Payload payload, TypeTag<? extends T> typeTag, ClassLoader classLoader, String mediaType,
            Map<String, String> options);

    Promise<Boolean> canDeserialize(TypeTag<?> typeTag, ClassLoader classLoader, String mediaType);

    Promise<Set<String>> deserializableMediaTypes(TypeTag<?> typeTag, ClassLoader classLoader);

    Promise<Set<String>> deserializableMediaTypes();

}
