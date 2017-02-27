package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import android.support.annotation.Nullable;

import java.util.Locale;

public class WebProductTest extends AndroidUnitTest {

    private Locale defaultLocale;

    @Before
    public void setUp() throws Exception {
        defaultLocale = Locale.getDefault();
    }

    @Test
    public void originalUsdFormattingForEnglishLocale() {
        setLocale(Locale.ENGLISH);

        final WebProduct product = buildWebProduct("$9.99", null, null);

        assertThat(product.getPrice()).isEqualTo("$9.99");
    }

    @Test
    public void originalUsdFormattingForUSSpanishLocale() {
        setLocale(new Locale("es", "US"));

        final WebProduct product = buildWebProduct("$9.99", null, null);

        assertThat(product.getPrice()).isEqualTo("$9.99");
    }

    @Test
    public void originalEurFormattingForEnglishLocale() {
        setLocale(Locale.ENGLISH);

        final WebProduct product = buildWebProduct("€9.99", null, null);

        assertThat(product.getPrice()).isEqualTo("€9.99");
    }

    @Test
    public void originalDiscountCurrencyFormattingForEnglishLocale() {
        setLocale(Locale.ENGLISH);

        final WebProduct product = buildWebProduct("€9.99", "€4.99", null);

        assertThat(product.getDiscountPrice().get()).isEqualTo("€4.99");
    }

    @Test
    public void originalNegativeProratingForEnglishLocale() {
        setLocale(Locale.ENGLISH);

        final WebProduct product = buildWebProduct("$9.99", null, "$-2.34");

        assertThat(product.getProratedPrice().get()).isEqualTo("$-2.34");
    }

    @Test
    public void reformattedCurrencyForFrance() {
        setLocale(Locale.FRENCH);

        final WebProduct product = buildWebProduct("€9.99", null, null);

        assertThat(product.getPrice()).isEqualTo("9,99 €");
    }

    @Test
    public void reformattedNegativeProratingForFrench() {
        setLocale(Locale.FRENCH);

        final WebProduct product = buildWebProduct("€9.99", null, "€-2.99");

        assertThat(product.getProratedPrice().get()).isEqualTo("-2,99 €");
    }

    @Test
    public void reformattedCurrencyForFrenchCanadian() {
        setLocale(Locale.CANADA_FRENCH);

        final WebProduct product = buildWebProduct("$9.99", null, null);

        assertThat(product.getPrice()).isEqualTo("9,99 $");
    }

    @Test
    public void reformattedDiscountCurrencyForFrance() {
        setLocale(Locale.FRENCH);

        final WebProduct product = buildWebProduct("€9.99", "€4.99", null);

        assertThat(product.getDiscountPrice().get()).isEqualTo("4,99 €");
    }

    @Test
    public void reformattedCurrencyForOtherNonEnglishLocale() {
        setLocale(Locale.GERMAN);

        final WebProduct product = buildWebProduct("€9.99", null, null);

        assertThat(product.getPrice()).isEqualTo("9,99€");
    }

    @Test
    public void reformattedDiscountCurrencyForOtherNonEnglishLocale() {
        setLocale(Locale.GERMAN);

        final WebProduct product = buildWebProduct("€9.99", "€4.99", null);

        assertThat(product.getDiscountPrice().get()).isEqualTo("4,99€");
    }

    @Test
    public void reformattedNegativeProratingForOtherNonEnglishLocale() {
        setLocale(Locale.GERMAN);

        final WebProduct product = buildWebProduct("€9.99", null, "€-1.23");

        assertThat(product.getProratedPrice().get()).isEqualTo("-1,23€");
    }

    @After
    public void tearDown() {
        Locale.setDefault(defaultLocale);
    }

    private WebProduct buildWebProduct(String price, @Nullable String discountedPrice, @Nullable String proratedPrice) {
        return WebProduct.create("high_tier", "package:123", price, discountedPrice, "9.99", "USD", 0, 0, null, proratedPrice, "123", "456");
    }

    private void setLocale(Locale locale) {
        Locale.setDefault(locale);
    }

}
