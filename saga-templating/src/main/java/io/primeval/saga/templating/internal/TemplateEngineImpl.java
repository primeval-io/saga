package io.primeval.saga.templating.internal;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.promise.Promise;

import io.primeval.codex.promise.PromiseHelper;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.templating.Template;
import io.primeval.saga.templating.TemplateEngine;
import io.primeval.saga.templating.spi.TemplateEngineProvider;

@Component(immediate = true)
public final class TemplateEngineImpl implements TemplateEngine {

    private final Map<String, TemplateEngineProvider> providers = new ConcurrentHashMap<>();

    @Override
    public Promise<Payload> render(Template template, Locale locale, Map<String, Object> variables) {
        return PromiseHelper.wrapPromise(() -> {
            TemplateEngineProvider engineProvider = providers.get(template.engine);
            if (engineProvider == null) {
                throw new RuntimeException("missing template engine: " + template.engine);
            }
            return engineProvider.render(template.name, template.owner, locale, variables);
        });
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addTemplateEngineProvider(TemplateEngineProvider provider) {
        providers.put(provider.engineName(), provider);
    }

    public void removeTemplateEngineProvider(TemplateEngineProvider provider) {
        providers.remove(provider.engineName(), provider);
    }

}
