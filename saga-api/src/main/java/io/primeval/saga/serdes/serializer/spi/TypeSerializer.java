package io.primeval.saga.serdes.serializer.spi;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.util.promise.Promise;

import io.primeval.saga.http.shared.Payload;

@FunctionalInterface
@ProviderType
public interface TypeSerializer<T> {
    Promise<Payload> serialize(T object, String mediaType, Map<String, String> options);
}