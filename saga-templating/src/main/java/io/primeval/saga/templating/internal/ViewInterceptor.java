package io.primeval.saga.templating.internal;

import java.util.Locale;
import java.util.Map;

import org.osgi.framework.BundleReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.promise.Promise;

import io.primeval.codex.promise.PromiseHelper;
import io.primeval.saga.action.ActionFunction;
import io.primeval.saga.action.ActionKey;
import io.primeval.saga.action.Context;
import io.primeval.saga.action.Result;
import io.primeval.saga.ext.locale.LocaleLookup;
import io.primeval.saga.guava.ImmutableResult;
import io.primeval.saga.http.protocol.HeaderNames;
import io.primeval.saga.interception.action.ActionInterceptor;
import io.primeval.saga.renderer.MimeTypes;
import io.primeval.saga.serdes.serializer.Serializable;
import io.primeval.saga.templating.Template;
import io.primeval.saga.templating.TemplateEngine;
import io.primeval.saga.templating.TemplateInstance;
import io.primeval.saga.templating.VariablesProvider;
import io.primeval.saga.templating.annotations.View;

@Component
public final class ViewInterceptor implements ActionInterceptor<View> {

    @Reference
    private TemplateEngine templateEngine;

    @Reference
    private LocaleLookup localeLookup;

    @Override
    public Promise<Result<?>> onAction(View templateDef, Context context, ActionKey actionKey,
            ActionFunction actionFunction) {
        return PromiseHelper.wrapPromise(() -> {
            String engine = templateDef.engine();
            if (engine.isEmpty()) {
                engine = findEngine(actionKey.classLoader());
            }
            Template template = templateEngine.createTemplate(templateDef.name(), engine, actionKey.classLoader());
            Locale locale = localeLookup.lookup(context);
            return actionFunction.apply(context).map(r -> {
                Serializable<?> ser = r.content().orElse(null);
                if (ser == null) {
                    throw new IllegalStateException("Cannot convert null result");
                }
                Object value = ser.value();
                VariablesProvider vars = create(value);
                TemplateInstance templateInstance = new TemplateInstance(template, vars, locale);

                return ImmutableResult.copySetupOf(r).withHeader(HeaderNames.CONTENT_TYPE, MimeTypes.HTML)
                        .setValue(templateInstance).build();
            });
        });

    }

    @SuppressWarnings("unchecked")
    private VariablesProvider create(Object value) {
        if (value instanceof Map) {
            // consider it's a Map<String, Object>, the only map we support
            return new IdentityVariablesProvider((Map<String, Object>) value);
        } else {
            return new ObjectVariablesProvider(value);
        }
    }

    private String findEngine(ClassLoader classLoader) {
        BundleReference br = (BundleReference) classLoader;
        String engineName = br.getBundle().getHeaders().get(TemplateEngine.HEADER_NAME);
        if (engineName == null) {
            throw new RuntimeException("Could not find templating engine, missing header " + TemplateEngine.HEADER_NAME
                    + " in bundle " + br.getBundle());
        }
        return engineName;
    }

    @Override
    public Class<View> type() {
        return View.class;
    }

}
