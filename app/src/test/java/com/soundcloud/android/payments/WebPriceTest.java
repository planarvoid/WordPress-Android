package com.soundcloud.android.payments;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

public class WebPriceTest {

    private Locale defaultLocale;

    @Before
    public void setUp() throws Exception {
        defaultLocale = Locale.getDefault();
    }

    @Test
    public void formatsUsdForEnglishLocale() {
        setLocale(Locale.ENGLISH);

        final WebPrice price = WebPrice.create(999, "USD");

        assertThat(price.format()).isEqualTo("$9.99");
    }

    @Test
    public void formatsUsdForUsSpanishLocale() {
        setLocale(new Locale("es", "US"));

        final WebPrice price = WebPrice.create(999, "USD");

        assertThat(price.format()).isEqualTo("$9.99");
    }

    @Test
    public void formatsEurForEnglishLocale() {
        setLocale(Locale.ENGLISH);

        final WebPrice price = WebPrice.create(999, "EUR");

        assertThat(price.format()).isEqualTo("€9.99");
    }

    @Test
    public void formatsNegativeProratingForEnglishLocale() {
        setLocale(Locale.ENGLISH);

        final WebPrice price = WebPrice.create(-234, "USD");

        assertThat(price.format()).isEqualTo("-$2.34");
    }

    @Test
    public void formatsEurForFrenchLocale() {
        setLocale(Locale.FRENCH);

        final WebPrice price = WebPrice.create(999, "EUR");

        assertThat(price.format()).isEqualTo("9,99 €");
    }

    @Test
    public void formatsNegativeProratingForFrenchLocale() {
        setLocale(Locale.FRENCH);

        final WebPrice price = WebPrice.create(-299, "EUR");

        assertThat(price.format()).isEqualTo("-2,99 €");
    }

    @Test
    public void formatsCadForFrenchCanadianLocale() {
        setLocale(Locale.CANADA_FRENCH);

        final WebPrice price = WebPrice.create(999, "CAD");

        assertThat(price.format()).isEqualTo("9,99 $");
    }

    @Test
    public void formatsEurForGermanLocale() {
        setLocale(Locale.GERMAN);

        final WebPrice price = WebPrice.create(999, "EUR");

        assertThat(price.format()).isEqualTo("€ 9,99");
    }

    @Test
    public void formatsNegativeProratingGermanLocale() {
        setLocale(Locale.GERMAN);

        final WebPrice price = WebPrice.create(-123, "EUR");

        assertThat(price.format()).isEqualTo("-€ 1,23");
    }

    @Test
    public void dropsEmptyDecimalPlaces() {
        setLocale(Locale.ENGLISH);

        final WebPrice price = WebPrice.create(500, "USD");

        assertThat(price.format()).isEqualTo("$5");
    }

    @Test
    public void maintainsTwoDecimalPlacesIfNotEmpty() {
        setLocale(Locale.ENGLISH);

        final WebPrice price = WebPrice.create(550, "USD");

        assertThat(price.format()).isEqualTo("$5.50");
    }

    @Test
    public void usesLocaleCurrencySymbolIfNotRecognised() {
        setLocale(Locale.JAPAN);

        final WebPrice price = WebPrice.create(10000, "JPY");

        assertThat(price.format()).isEqualTo("￥100");
    }

    @Test
    public void usesCurrencyCodeIfNotMappedToSymbol() {
        setLocale(Locale.US);

        final WebPrice price = WebPrice.create(1000, "IDK");

        assertThat(price.format()).isEqualTo("IDK10");
    }

    @Test
    public void formatsDecimalAmountForTracking() {
        assertThat(WebPrice.create(999, "USD").decimalString()).isEqualTo("9.99");
        assertThat(WebPrice.create(900, "USD").decimalString()).isEqualTo("9.00");
        assertThat(WebPrice.create(950, "USD").decimalString()).isEqualTo("9.50");
    }

    @After
    public void tearDown() {
        Locale.setDefault(defaultLocale);
    }

    private void setLocale(Locale locale) {
        Locale.setDefault(locale);
    }

}
