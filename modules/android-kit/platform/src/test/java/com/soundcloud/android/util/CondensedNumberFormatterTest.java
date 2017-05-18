package com.soundcloud.android.util;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

public class CondensedNumberFormatterTest extends AndroidUnitTest {

    private CondensedNumberFormatter formatter;

    @Before
    public void setUp() {
        formatter = new CondensedNumberFormatter(Locale.US, resources());
    }

    @Test
    public void formatOneDigitNumberCorrectly() {
        String formattedNumber = formatter.format(9L);

        assertThat(formattedNumber).isEqualTo("9");
    }

    @Test
    public void formatTwoDigitNumberCorrectly() {
        String formattedNumber = formatter.format(89L);

        assertThat(formattedNumber).isEqualTo("89");
    }

    @Test
    public void formatThreeDigitNumberCorrectly() {
        String formattedNumber = formatter.format(789L);

        assertThat(formattedNumber).isEqualTo("789");
    }

    @Test
    public void formatFourDigitNumberUsingGroupingSeparator() {
        String formattedNumber = formatter.format(6789L);

        assertThat(formattedNumber).isEqualTo("6,789");
    }

    @Test
    public void formatFiveDigitNumberUsingOneDecimalDigitAndThousandsSuffix() {
        String formattedNumber = formatter.format(56789L);

        assertThat(formattedNumber).isEqualTo("56.7K");
    }

    @Test
    public void formatSixDigitNumberUsingThousandsSuffixWithoutDecimalDigits() {
        String formattedNumber = formatter.format(456789L);

        assertThat(formattedNumber).isEqualTo("456K");
    }

    @Test
    public void formatSevenDigitNumberUsingTwoDecimalDigitsAndMillionsSuffix() {
        String formattedNumber = formatter.format(3456789L);

        assertThat(formattedNumber).isEqualTo("3.45M");
    }

    @Test
    public void formatEightDigitNumberUsingOneDecimalDigitAndMillionsSuffix() {
        String formattedNumber = formatter.format(23456789L);

        assertThat(formattedNumber).isEqualTo("23.4M");
    }

    @Test
    public void formatNineDigitNumberUsingMillionsSuffixWithoutDecimalDigits() {
        String formattedNumber = formatter.format(123456789L);

        assertThat(formattedNumber).isEqualTo("123M");
    }

    @Test
    public void formatTenDigitNumberUsingTwoDecimalDigitsAndBillionsSuffix() {
        String formattedNumber = formatter.format(9123456789L);

        assertThat(formattedNumber).isEqualTo("9.12B");
    }

    @Test
    public void formatElevenDigitNumberUsingOneDecimalDigitAndBillionsSuffix() {
        String formattedNumber = formatter.format(89123456789L);

        assertThat(formattedNumber).isEqualTo("89.1B");
    }

    @Test
    public void formatTwelveDigitNumberUsingBillionsSuffixWithoutDecimalDigits() {
        String formattedNumber = formatter.format(789123456789L);

        assertThat(formattedNumber).isEqualTo("789B");
    }

    @Test
    public void formatLeadingZeroNumberWithOneDigit() {
        String formattedNumber = formatter.format(1204567L);

        assertThat(formattedNumber).isEqualTo("1.2M"); // not 1.20M
    }

    @Test
    public void formatLeadingZeroNumberWithTwoDigit() {
        String formattedNumber = formatter.format(12045678L);

        assertThat(formattedNumber).isEqualTo("12M");  // not 12.0M
    }

    @Test
    public void formatLeadingZeroNumberWithThreeDigit() {
        String formattedNumber = formatter.format(120456789L);

        assertThat(formattedNumber).isEqualTo("120M");
    }

    @Test
    public void formatsLastNonTruncatedNumberCorrectly() {
        String formattedNumber = formatter.format(9999L);

        assertThat(formattedNumber).isEqualTo("9,999");
    }

    @Test
    public void formatsFirstTruncatedNumberCorrectly() {
        String formattedNumber = formatter.format(10000L);

        assertThat(formattedNumber).isEqualTo("10K");
    }

}
