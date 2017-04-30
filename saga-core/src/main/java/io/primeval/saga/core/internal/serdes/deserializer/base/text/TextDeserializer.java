package io.primeval.saga.core.internal.serdes.deserializer.base.text;

import org.osgi.service.component.annotations.Component;
import org.osgi.util.promise.Promise;

import io.primeval.codex.promise.PromiseHelper;
import io.primeval.common.serdes.DeserializationException;
import io.primeval.common.type.TypeTag;
import io.primeval.saga.renderer.MimeTypes;
import io.primeval.saga.serdes.SupportsMediaTypes;
import io.primeval.saga.serdes.deserializer.spi.MediaDeserializer;
import io.primeval.saga.serdes.deserializer.spi.TypeDeserializer;

@Component
@SupportsMediaTypes(MimeTypes.TEXT)
public final class TextDeserializer implements MediaDeserializer {

    @SuppressWarnings("unchecked")
    @Override
    public <T> Promise<TypeDeserializer<T>> typeDeserializer(TypeTag<? extends T> typeTag, ClassLoader classLoader) {
        return PromiseHelper.wrap(() -> {
            if (typeTag.rawType() == String.class) {
                return (TypeDeserializer<T>) new StringDeserializer();
            }
            throw new DeserializationException(typeTag, new UnsupportedOperationException());
        });

    }

}
