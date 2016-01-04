package com.soundcloud.android.utils;

import com.soundcloud.java.optional.Optional;

import javax.inject.Inject;
import java.util.Locale;

public class LocaleHeaderFormatter {

    private final Locale locale;

    @Inject
    public LocaleHeaderFormatter(Locale defaultLocale) {
        this.locale = defaultLocale;
    }

    // Format using only language & country: {language}["-"{country}]
    // according to IETF standard BCP 47
    public Optional<String> getFormattedLocale() {
        if (!locale.getLanguage().isEmpty() && !locale.getCountry().isEmpty()) {
            return Optional.of(locale.getLanguage() + "-" + locale.getCountry());
        } else if (!locale.getLanguage().isEmpty()) {
            return Optional.of(locale.getLanguage());
        } else {
            return Optional.absent();
        }
    }
}
