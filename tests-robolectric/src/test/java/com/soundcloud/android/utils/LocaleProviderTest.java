package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

@RunWith(SoundCloudTestRunner.class)
public class LocaleProviderTest {

    @Test
    public void shouldReturnValidLocaleParameterWithLangAndCountry() {
        setLocale("en", "CA", "");
        expect(LocaleProvider.getFormattedLocale()).toEqual("en-CA");
    }

    @Test
    public void shouldReturnOnlyLanguageIfCountryMissing() {
        setLocale("en", "", "");

        expect(LocaleProvider.getFormattedLocale()).toEqual("en");
    }

    @Test
    public void shouldNotReturnVariantInValidLocale() {
        setLocale("sl", "IT", "nedis");

        expect(LocaleProvider.getFormattedLocale()).toEqual("sl-IT");
    }

    @Test
    public void shouldNotReturnLocaleParameterIfMissingLanguage() {
        setLocale("", "US", "");

        expect(LocaleProvider.getFormattedLocale()).toEqual("");
    }

    @Test
    public void shouldNotReturnLocaleParameterIfAllComponentsMissing() {
        setLocale("", "", "");

        expect(LocaleProvider.getFormattedLocale()).toEqual("");
    }

    private void setLocale(String language, String country, String variant) {
        Locale locale = new Locale(language, country, variant);
        Locale.setDefault(locale);
    }
}
