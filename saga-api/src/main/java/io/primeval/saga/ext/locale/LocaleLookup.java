package io.primeval.saga.ext.locale;

import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;

import io.primeval.saga.action.Context;
import io.primeval.saga.http.protocol.HeaderNames;

public interface LocaleLookup {

    public Locale lookup(Context context);

    public static Locale lookup(Context context, Locale defaultLocale, List<Locale> supportedLocales) {
        List<String> reqLangs = context.request().headers.get(HeaderNames.ACCEPT_LANGUAGE);
        String acceptLang = (reqLangs != null && !reqLangs.isEmpty()) ? reqLangs.get(0) : null;
        if (acceptLang != null) {
            List<LanguageRange> ranges = LanguageRange.parse(acceptLang);
            Locale lookup = Locale.lookup(ranges, supportedLocales);
            if (lookup == null) {
                return defaultLocale;
            }
            return lookup;

        } else {
            return defaultLocale;
        }
    }
}
