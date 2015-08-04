package com.soundcloud.android.utils;

import static org.junit.Assert.assertTrue;

import java.util.Locale;
import org.junit.Test;

public class LocaleProviderTest {

    @Test
    public void shouldReturnValidLocaleParameterWithLangAndCountry() {
        setLocale("en", "CA", "");
        assertTrue(LocaleProvider.getFormattedLocale().equals("en-CA"));
    }

    @Test
    public void shouldReturnOnlyLanguageIfCountryMissing() {
        setLocale("en", "", "");

        assertTrue(LocaleProvider.getFormattedLocale().equals("en"));
    }

    @Test
    public void shouldNotReturnVariantInValidLocale() {
        setLocale("sl", "IT", "nedis");

        assertTrue(LocaleProvider.getFormattedLocale().equals("sl-IT"));
    }

    @Test
    public void shouldNotReturnLocaleParameterIfMissingLanguage() {
        setLocale("", "US", "");

        assertTrue(LocaleProvider.getFormattedLocale().isEmpty());
    }

    @Test
    public void shouldNotReturnLocaleParameterIfAllComponentsMissing() {
        setLocale("", "", "");

        assertTrue(LocaleProvider.getFormattedLocale().isEmpty());
    }

    private void setLocale(String language, String country, String variant) {
        Locale locale = new Locale(language, country, variant);
        Locale.setDefault(locale);
    }
}