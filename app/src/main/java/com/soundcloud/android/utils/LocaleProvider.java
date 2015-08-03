package com.soundcloud.android.utils;

import java.util.Locale;

public final class LocaleProvider {

    private LocaleProvider() {}

    // BCP 47 Format using only language & country: {language}["-"{country}]
    public static String getFormattedLocale() {
        final Locale locale = Locale.getDefault();

        if (!locale.getLanguage().isEmpty() && !locale.getCountry().isEmpty()) {
            return locale.getLanguage() + "-" + locale.getCountry();
        } else if (!locale.getLanguage().isEmpty()) {
            return locale.getLanguage();
        } else {
            return "";
        }
    }
}
