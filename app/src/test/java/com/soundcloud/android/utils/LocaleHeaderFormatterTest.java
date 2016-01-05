package com.soundcloud.android.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.Locale;

public class LocaleHeaderFormatterTest {

    @Test
    public void shouldReturnValidLocaleParameterWithLangAndCountry() {
        LocaleHeaderFormatter formatter = new LocaleHeaderFormatter(Locale.CANADA);

        assertThat(formatter.getFormattedLocale().get()).isEqualTo("en-CA");
    }

    @Test
    public void shouldReturnOnlyLanguageIfCountryMissing() {
        LocaleHeaderFormatter formatter = new LocaleHeaderFormatter(Locale.ENGLISH);

        assertThat(formatter.getFormattedLocale().get()).isEqualTo("en");
    }

    @Test
    public void shouldNotReturnVariantInValidLocale() {
        LocaleHeaderFormatter formatter = new LocaleHeaderFormatter(new Locale("sl", "IT", "nedis"));

        assertThat(formatter.getFormattedLocale().get()).isEqualTo("sl-IT");
    }

    @Test
    public void shouldNotReturnLocaleParameterIfMissingLanguage() {
        LocaleHeaderFormatter formatter = new LocaleHeaderFormatter(new Locale("", "US", ""));

        assertThat(formatter.getFormattedLocale().isPresent()).isFalse();
    }

    @Test
    public void shouldNotReturnLocaleParameterIfAllComponentsMissing() {
        LocaleHeaderFormatter formatter = new LocaleHeaderFormatter(new Locale("", "", ""));

        assertThat(formatter.getFormattedLocale().isPresent()).isFalse();
    }

}
