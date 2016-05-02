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
        setLocale("en");

        final WebProduct product = buildWebProduct("$9.99", null);

        assertThat(product.getPrice()).isEqualTo("$9.99");
    }

    @Test
    public void originalEurFormattingForEnglishLocale() {
        setLocale("en");

        final WebProduct product = buildWebProduct("€9.99", null);

        assertThat(product.getPrice()).isEqualTo("€9.99");
    }

    @Test
    public void originalDiscountCurrencyFormattingForEnglishLocale() {
        setLocale("en");

        final WebProduct product = buildWebProduct("€9.99", "€4.99");

        assertThat(product.getDiscountPrice().get()).isEqualTo("€4.99");
    }

    @Test
    public void reformattedCurrencyForNonEnglishLocale() {
        setLocale("fr");

        final WebProduct product = buildWebProduct("€9.99", null);

        assertThat(product.getPrice()).isEqualTo("9,99€");
    }

    @Test
    public void reformattedDiscountCurrencyForNonEnglishLocale() {
        setLocale("fr");

        final WebProduct product = buildWebProduct("€9.99", "€4.99");

        assertThat(product.getDiscountPrice().get()).isEqualTo("4,99€");
    }

    @After
    public void tearDown() {
        Locale.setDefault(defaultLocale);
    }

    private WebProduct buildWebProduct(String price, @Nullable String discountedPrice) {
        return WebProduct.create("high_tier", "package:123", price, discountedPrice, "9.99", "USD", 0, "123", "456");
    }

    private void setLocale(String code) {
        Locale.setDefault(new Locale(code));
    }

}
