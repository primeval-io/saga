package io.primeval.saga.thymeleaf.internal;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.util.promise.Promise;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.IContext;

import com.google.common.collect.ImmutableMap;

import io.primeval.codex.io.IODispatcher;
import io.primeval.codex.promise.PromiseHelper;
import io.primeval.common.classloading.CompositeClassLoader;
import io.primeval.compendium.i18n.I18n;
import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.serdes.serializer.Serializer;
import io.primeval.saga.templating.spi.TemplateEngineProvider;
import io.primeval.saga.thymeleaf.ThymeleafTemplateEngine;
import reactor.core.publisher.Mono;

@Component
public final class ThymeleafTemplateEngineImpl implements TemplateEngineProvider, ThymeleafTemplateEngine {

    public static final String CLASSLOADER_VAR = "__CLASSLOADER__";
    private org.thymeleaf.TemplateEngine engine;

    @Reference
    private IODispatcher ioDispatcher;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private volatile I18n i18n;

    @Reference
    private Serializer serializer;

    @Activate
    public void activate() {
        engine = new org.thymeleaf.TemplateEngine();
        engine.setDialect(new SagaDialect());
        engine.setTemplateResolver(new SagaTemplateResolver());
        engine.setMessageResolver(new SagaMessageResolver(() -> i18n));
    }

    @Override
    public Promise<Payload> render(String templateName, ClassLoader classLoader, Locale locale,
            Map<String, Object> variables) {
        HashMap<String, Object> vars = new HashMap<>(variables);
        CompositeClassLoader processClassLoader = new CompositeClassLoader(new ClassLoader[] {
                ThymeleafTemplateEngineImpl.class.getClassLoader(),
                classLoader });
        vars.put(CLASSLOADER_VAR, classLoader);
        return ioDispatcher.dispatch(() -> {
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            ImmutableMap<String, Object> templateResolutionAttributes = ImmutableMap
                    .of(CLASSLOADER_VAR, classLoader);
            try {

                Thread.currentThread().setContextClassLoader(processClassLoader);
                return engine.process(new TemplateSpec(templateName, templateResolutionAttributes),
                        new TemplateContext(vars, locale));
            } finally {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }

        }).flatMap(res -> PromiseHelper.wrap(() -> {
            byte[] b = res.toString().getBytes(StandardCharsets.UTF_8.name());
            return Payload.ofLength(b.length, Mono.just(ByteBuffer.wrap(b)));
        }));

    }
}

final class TemplateContext implements IContext {

    private final Map<String, Object> vars;
    private final Locale locale;

    public TemplateContext(Map<String, Object> vars, Locale locale) {
        this.vars = vars;
        this.locale = locale;
    }

    @Override
    public Set<String> getVariableNames() {
        return vars.keySet();
    }

    @Override
    public Object getVariable(String name) {
        return vars.get(name);
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public boolean containsVariable(String name) {
        return vars.containsKey(name);
    }
}