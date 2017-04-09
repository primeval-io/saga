package io.primeval.saga.serdes.deserializer.spi;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.util.promise.Promise;

import io.primeval.saga.http.shared.Payload;

@FunctionalInterface
@ProviderType
public interface TypeDeserializer<T> {
    Promise<T> deserialize(Payload payload, String mimeType, Map<String, String> options);
}