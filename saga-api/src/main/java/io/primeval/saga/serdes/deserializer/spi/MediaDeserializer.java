package io.primeval.saga.serdes.deserializer.spi;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.util.promise.Promise;

import io.primeval.common.type.TypeTag;

@FunctionalInterface
@ProviderType
public interface MediaDeserializer {

    // If the promise is successful, the payload SHOULD be deserializable;
    <T> Promise<TypeDeserializer<T>> typeDeserializer(TypeTag<? extends T> typeTag, ClassLoader classLoader);

}
