package io.primeval.saga.core.internal.serdes.serializer.base.text;

import org.osgi.service.component.annotations.Component;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

import io.primeval.common.type.TypeTag;
import io.primeval.saga.renderer.MimeTypes;
import io.primeval.saga.serdes.SupportsMediaTypes;
import io.primeval.saga.serdes.serializer.spi.MediaSerializer;
import io.primeval.saga.serdes.serializer.spi.TypeSerializer;

@Component
@SupportsMediaTypes(MimeTypes.TEXT)
public final class TextSerializer implements MediaSerializer {

    private static final Promise<TypeSerializer<?>> SERIALIZER = Promises.resolved(new ObjectToTextSerializer());

    @SuppressWarnings("unchecked")
    @Override
    public <T> Promise<TypeSerializer<T>> typeSerializer(TypeTag<? extends T> typeTag) {
        return (Promise) SERIALIZER;

    }

}
