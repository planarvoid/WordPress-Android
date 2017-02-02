package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

public class ProductInfoFormatterTest extends AndroidUnitTest{

    private ProductInfoFormatter formatter;

    @Before
    public void setUp() throws Exception {
        formatter = new ProductInfoFormatter(resources());
    }

    @Test
    public void formatMonthlyPricing() {
        assertThat(formatter.monthlyPricing("$9.99")).isEqualTo("$9.99 / month");
    }

    @Test
    public void formatPromoPricing() {
        assertThat(formatter.promoPricing(30, "$1.99")).isEqualTo("1 month for $1.99");
    }

    @Test
    public void formatPromoDurationMonths() {
        assertThat(formatter.promoDuration(15)).isEqualTo("15 days");
        assertThat(formatter.promoDuration(30)).isEqualTo("1 month");
        assertThat(formatter.promoDuration(90)).isEqualTo("3 months");
        assertThat(formatter.promoDuration(123)).isEqualTo("4 months");
    }

    @Test
    public void configuresDefaultRestrictionsTextWithNoPromo() {
        assertThat(formatter.configuredRestrictionsText(TestProduct.highTier()))
                .isEqualTo("Restrictions apply");
    }

    @Test
    public void configuresPromoRestrictionsTextWithPromo() {
        assertThat(formatter.configuredRestrictionsText(TestProduct.highTierPromo()))
                .isEqualTo("Restrictions apply, $2 after");
    }

}
