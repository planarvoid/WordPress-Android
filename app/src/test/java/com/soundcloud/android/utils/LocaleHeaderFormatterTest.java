package com.soundcloud.android.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.Locale;

public class LocaleHeaderFormatterTest {

    @Test
    public void shouldReturnValidLocaleParameterWithLangAndCountry() {
        LocaleFormatter formatter = new LocaleFormatter(Locale.CANADA);

        assertThat(formatter.getLocale().get()).isEqualTo("en-CA");
    }

    @Test
    public void shouldReturnOnlyLanguageIfCountryMissing() {
        LocaleFormatter formatter = new LocaleFormatter(Locale.ENGLISH);

        assertThat(formatter.getLocale().get()).isEqualTo("en");
    }

    @Test
    public void shouldNotReturnVariantInValidLocale() {
        LocaleFormatter formatter = new LocaleFormatter(new Locale("sl", "IT", "nedis"));

        assertThat(formatter.getLocale().get()).isEqualTo("sl-IT");
    }

    @Test
    public void shouldNotReturnLocaleParameterIfMissingLanguage() {
        LocaleFormatter formatter = new LocaleFormatter(new Locale("", "US", ""));

        assertThat(formatter.getLocale().isPresent()).isFalse();
    }

    @Test
    public void shouldNotReturnLocaleParameterIfAllComponentsMissing() {
        LocaleFormatter formatter = new LocaleFormatter(new Locale("", "", ""));

        assertThat(formatter.getLocale().isPresent()).isFalse();
    }

}
