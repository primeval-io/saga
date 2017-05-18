package io.primeval.saga.templating.spi;

import java.util.Locale;
import java.util.Map;

import org.osgi.util.promise.Promise;

import io.primeval.saga.http.shared.Payload;
import io.primeval.saga.templating.Template;

public interface TemplateEngineProvider {

    Promise<Payload> render(String templateName, ClassLoader classLoader, Locale locale,
            Map<String, Object> variables);

    String engineName();

    default Template createTemplate(String templateName, ClassLoader classLoader) {
        return new Template(templateName, engineName(), classLoader);
    }

}
