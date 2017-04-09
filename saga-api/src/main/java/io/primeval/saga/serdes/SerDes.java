package io.primeval.saga.serdes;

import java.util.Set;

import org.osgi.util.promise.Promise;

import io.primeval.common.type.TypeTag;
import io.primeval.saga.serdes.deserializer.Deserializer;
import io.primeval.saga.serdes.serializer.Serializer;

public interface SerDes {

    Promise<Set<String>> serDesMediaTypes(TypeTag<?> typeTag, ClassLoader classLoader);

    Serializer serializer();
    
    Deserializer deserializer();
}
