package io.primeval.saga.thymeleaf.internal;

import java.util.Map;

import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresource.ClassLoaderTemplateResource;
import org.thymeleaf.templateresource.ITemplateResource;

public final class SagaTemplateResolver extends AbstractConfigurableTemplateResolver {

    public SagaTemplateResolver() {
        setPrefix("templates/");
        setSuffix(".thl.html");
    }

    @Override
    protected ITemplateResource computeTemplateResource(IEngineConfiguration configuration, String ownerTemplate,
            String template, String resourceName, String characterEncoding,
            Map<String, Object> templateResolutionAttributes) {
        ClassLoader classloader = (ClassLoader) templateResolutionAttributes
                .get(ThymeleafTemplateEngineImpl.CLASSLOADER_VAR);
        return new ClassLoaderTemplateResource(classloader, resourceName, characterEncoding);
    }

}
