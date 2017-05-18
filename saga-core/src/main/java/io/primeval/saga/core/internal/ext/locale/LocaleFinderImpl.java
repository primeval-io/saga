package io.primeval.saga.core.internal.ext.locale;

import java.util.Locale;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.primeval.compendium.i18n.I18n;
import io.primeval.saga.action.Context;
import io.primeval.saga.ext.locale.LocaleLookup;

@Component
public final class LocaleFinderImpl implements LocaleLookup {

    @Reference
    protected I18n i18n;

    @Override
    public Locale lookup(Context context) {
        return LocaleLookup.lookup(context, i18n.defaultLocale(), i18n.supportedLocales());
    }

}
