package io.primeval.saga.core.internal.serdes;

import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.promise.Promise;

import com.google.common.collect.Sets;

import io.primeval.common.type.TypeTag;
import io.primeval.saga.serdes.SerDes;
import io.primeval.saga.serdes.deserializer.Deserializer;
import io.primeval.saga.serdes.serializer.Serializer;

@Component
public final class SerDesImpl implements SerDes {

    private Serializer serializer;
    private Deserializer deserializer;

    @Override
    public Promise<Set<String>> serDesMediaTypes(TypeTag<?> typeTag, ClassLoader classLoader) {
        Promise<Set<String>> serializableMediaTypesPms = serializer.serializableMediaTypes(typeTag);
        Promise<Set<String>> deserializableMediaTypesPms = deserializer.deserializableMediaTypes(typeTag, classLoader);

        return serializableMediaTypesPms
                .flatMap(serializableMediaTypes -> deserializableMediaTypesPms.map(deserializableMediaTypes -> {
                    return Sets.intersection(serializableMediaTypes, deserializableMediaTypes);
                }));
    }

    @Reference
    public void setSerializer(Serializer serializer) {
        this.serializer = serializer;
    }

    @Reference
    public void setDeserializer(Deserializer deserializer) {
        this.deserializer = deserializer;
    }
    
    @Override
    public Deserializer deserializer() {
        return deserializer;
    }
    
    @Override
    public Serializer serializer() {
        return serializer;
    }

}
