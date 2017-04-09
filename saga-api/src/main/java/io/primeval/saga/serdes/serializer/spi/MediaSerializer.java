package io.primeval.saga.serdes.serializer.spi;

import org.osgi.util.promise.Promise;

import io.primeval.common.type.TypeTag;

public interface MediaSerializer {

    // If the promise is successful, the payload SHOULD be serializable;
    <T> Promise<TypeSerializer<T>> typeSerializer(TypeTag<? extends T> typeTag);
}
