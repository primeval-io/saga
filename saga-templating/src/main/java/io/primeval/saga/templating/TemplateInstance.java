package io.primeval.saga.templating;

import java.util.Locale;

public final class TemplateInstance {

    public final Template template;
    
    public final VariablesProvider variablesProvider;
    
    public final Locale locale;

    public TemplateInstance(Template template, VariablesProvider variablesProvider, Locale locale) {
        this.template = template;
        this.variablesProvider = variablesProvider;
        this.locale = locale;
    }
    
    
    
}
