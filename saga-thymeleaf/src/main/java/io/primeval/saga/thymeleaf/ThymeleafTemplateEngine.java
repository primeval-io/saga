package io.primeval.saga.thymeleaf;

import io.primeval.saga.templating.Template;
import io.primeval.saga.templating.spi.TemplateEngineProvider;

public interface ThymeleafTemplateEngine extends TemplateEngineProvider {

    public static final String ENGINE_NAME = "thymeleaf";

    public static Template newTemplate(String name, ClassLoader owner) {
        return new Template(name, ENGINE_NAME, owner);
    }

    default String engineName() {
        return ENGINE_NAME;
    }
}
