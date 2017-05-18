package io.primeval.saga.core.internal.serdes.serializer.base.html;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Promises;

import com.google.common.collect.Maps;

import io.primeval.common.type.TypeTag;
import io.primeval.saga.core.internal.serdes.serializer.base.text.ObjectToTextSerializer;
import io.primeval.saga.renderer.MimeTypes;
import io.primeval.saga.serdes.SupportsMediaTypes;
import io.primeval.saga.serdes.serializer.spi.MediaSerializer;
import io.primeval.saga.serdes.serializer.spi.TypeSerializer;
import io.primeval.saga.serdes.serializer.spi.TypeSerializerProvider;

@Component
@SupportsMediaTypes(MimeTypes.HTML)
public final class HtmlSerializer implements MediaSerializer {

    private static final Promise<TypeSerializer<?>> SERIALIZER = Promises.resolved(new ObjectToTextSerializer());

    private final Map<TypeTag<?>, TypeSerializerProvider<?>> serializers = Maps.newConcurrentMap();

    @SuppressWarnings("unchecked")
    @Override
    public <T> Promise<TypeSerializer<T>> typeSerializer(TypeTag<? extends T> typeTag) {
        // TODO better, hierarchical check for matching TypeTags?
        TypeSerializerProvider<?> provider = serializers.get(typeTag);
        if (provider != null) {
            return Promises.resolved((TypeSerializer<T>) provider);
        }
        return (Promise) SERIALIZER;

    }

    @Reference(target = "(saga.serdes.mimetype=text/html)", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addTypeSerializer(TypeSerializerProvider<?> typeSerializer) {
        serializers.put(typeSerializer.type(), typeSerializer);
    }

    public void removeTypeSerializer(TypeSerializerProvider<?> typeSerializer) {
        serializers.remove(typeSerializer.type(), typeSerializer);
    }
}
