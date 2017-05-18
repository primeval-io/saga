package io.primeval.saga.templating.internal;

import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.promise.Promise;

import io.primeval.common.type.TypeTag;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.renderer.MimeTypes;
import io.primeval.saga.serdes.SupportsMediaTypes;
import io.primeval.saga.serdes.serializer.spi.TypeSerializerProvider;
import io.primeval.saga.templating.TemplateEngine;
import io.primeval.saga.templating.TemplateInstance;

@Component
@SupportsMediaTypes(MimeTypes.HTML)
public final class TemplateInstanceSerializer implements TypeSerializerProvider<TemplateInstance> {

    @Reference
    private TemplateEngine templateEngine;

    @Override
    public Promise<Payload> serialize(TemplateInstance templateInstance, String mediaType,
            Map<String, String> options) {
        return templateEngine.render(templateInstance);
    }

    @Override
    public TypeTag<TemplateInstance> type() {
        return TypeTag.of(TemplateInstance.class);
    }

}
