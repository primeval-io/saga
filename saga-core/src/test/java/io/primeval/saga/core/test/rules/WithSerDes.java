package io.primeval.saga.core.test.rules;

import java.util.function.Supplier;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.primeval.common.test.rules.TestResource;
import io.primeval.saga.core.internal.serdes.SerDesImpl;
import io.primeval.saga.core.internal.serdes.deserializer.DeserializerImpl;
import io.primeval.saga.core.internal.serdes.deserializer.base.text.TextDeserializer;
import io.primeval.saga.core.internal.serdes.serializer.SerializerImpl;
import io.primeval.saga.core.internal.serdes.serializer.base.text.TextSerializer;
import io.primeval.saga.serdes.SupportsMediaTypes;
import io.primeval.saga.serdes.deserializer.spi.MediaDeserializer;
import io.primeval.saga.serdes.serializer.spi.MediaSerializer;

public class WithSerDes extends ExternalResource implements TestResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(WithSerDes.class);

    private SerDesImpl serDes;
    private SerializerImpl serializer;
    private DeserializerImpl deserializer;
    private Supplier<?>[] serDesSuppliers;

    public WithSerDes(Supplier<?>... serDesSuppliers) {
        this.serDesSuppliers = serDesSuppliers;
    }

    @Override
    public void before() throws Throwable {
        this.serializer = new SerializerImpl();
        TextSerializer textSerializer = new TextSerializer();
        addMediaSerializerOrDeserializer(textSerializer);

        this.deserializer = new DeserializerImpl();
        TextDeserializer textDeserializer = new TextDeserializer();
        addMediaSerializerOrDeserializer(textDeserializer);

        for (Supplier<?> sds : serDesSuppliers) {
            Object sd = sds.get();
            addMediaSerializerOrDeserializer(sd);
        }

        serDes = new SerDesImpl();
        serDes.setDeserializer(deserializer);
        serDes.setSerializer(serializer);

    }

    private void addMediaSerializerOrDeserializer(Object sd) {
        boolean ser = sd instanceof MediaSerializer;
        boolean deser = sd instanceof MediaDeserializer;
        SupportsMediaTypes supportsMimeTypes = sd.getClass().getAnnotation(SupportsMediaTypes.class);
        if (!ser && !deser) {
            LOGGER.warn(
                    "Got an object of type {} that is neither a serializer or a deserializer, ignoring!",
                    sd.getClass());
            return;
        }
        if (supportsMimeTypes == null) {
            LOGGER.warn(
                    "Cannot find mimetypes associated serializer/deserializer of type {}, is the @SupportsMimeTypes annotation present? ignoring!",
                    sd.getClass());
            return;
        }

        for (String mt : supportsMimeTypes.value()) {
            if (ser) {
                this.serializer.addMimeTypeSerializer(mt, (MediaSerializer) sd);
            }
            if (deser) {
                this.deserializer.addMimeTypeDeserializer(mt, (MediaDeserializer) sd);
            }
        }
    }

    @Override
    public void after() {
    }

    public SerDesImpl getSerDes() {
        return serDes;
    }

    public SerializerImpl getSerializer() {
        return serializer;
    }

    public DeserializerImpl getDeserializer() {
        return deserializer;
    }
}
