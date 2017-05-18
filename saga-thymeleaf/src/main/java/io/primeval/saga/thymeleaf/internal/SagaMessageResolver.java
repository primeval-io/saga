package io.primeval.saga.thymeleaf.internal;

import java.util.function.Supplier;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.messageresolver.IMessageResolver;

import io.primeval.compendium.i18n.I18n;

public final class SagaMessageResolver implements IMessageResolver {

    private final Supplier<I18n> i18nSupplier;

    public SagaMessageResolver(Supplier<I18n> i18nSupplier) {
        this.i18nSupplier = i18nSupplier;
    }

    @Override
    public String getName() {
        return "Saga";
    }

    @Override
    public Integer getOrder() {
        return 0;
    }

    @Override
    public String resolveMessage(ITemplateContext context, Class<?> origin, String key, Object[] messageParameters) {
        I18n i18n = i18nSupplier.get();
        if (i18n != null) {
            return i18n.get(context.getLocale(), key, messageParameters);
        }
        return null;
    }

    @Override
    public String createAbsentMessageRepresentation(ITemplateContext context, Class<?> origin, String key,
            Object[] messageParameters) {
        if (context.getLocale() != null) {
            return "??"+key+"_" + context.getLocale().toString() + "??";
        }
        return "??"+key+"_" + "??";
    }

}
