package io.primeval.saga.serdes.serializer.spi;

import org.osgi.annotation.versioning.ProviderType;

import io.primeval.common.type.TypeTag;

@ProviderType
public interface TypeSerializerProvider<T> extends TypeSerializer<T> {

    TypeTag<T> type();
}