package io.primeval.saga.templating;

import java.util.Locale;
import java.util.Map;

import org.osgi.util.promise.Promise;

import io.primeval.saga.http.shared.Payload;

public interface TemplateEngine {

    public static final String HEADER_NAME = "Template-Engine";

    Promise<Payload> render(Template template, Locale locale, Map<String, Object> variables);

    default Promise<Payload> render(TemplateInstance templateInstance) {
        return render(templateInstance.template, templateInstance.locale,
                templateInstance.variablesProvider.getVariables());
    }

    default Template createTemplate(String templateName, String engineName, ClassLoader classLoader) {
        return new Template(templateName, engineName, classLoader);
    }

}
