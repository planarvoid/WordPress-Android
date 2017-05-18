package com.soundcloud.android.util;

import com.soundcloud.androidkit.R;

import android.content.res.Resources;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class CondensedNumberFormatter {

    private static final String SINGLE_DECIMAL_PATTERN = "##.#";
    private static final String DOUBLE_DECIMAL_PATTERN = "#.##";
    private static final String NO_DECIMAL_PATTERN = "#,###";
    private static final double TEN = 10.0;
    private static final double ONE_HUNDRED = 100.0;
    private static final double ONE_THOUSAND = 1000.0;

    private final DecimalFormat singleDecimalFormat;
    private final DecimalFormat doubleDecimalFormat;
    private final DecimalFormat noDecimalFormat;
    private final String[] suffixes;

    public static CondensedNumberFormatter create(Locale locale, Resources resources) {
        return new CondensedNumberFormatter(locale, resources);
    }

    CondensedNumberFormatter(Locale locale, Resources resources) {
        DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(locale);
        noDecimalFormat = createFormatter(NO_DECIMAL_PATTERN, formatSymbols);
        singleDecimalFormat = createFormatter(SINGLE_DECIMAL_PATTERN, formatSymbols);
        doubleDecimalFormat = createFormatter(DOUBLE_DECIMAL_PATTERN, formatSymbols);
        suffixes = resources.getStringArray(R.array.ak_number_suffixes);
    }

    private DecimalFormat createFormatter(String pattern, DecimalFormatSymbols formatSymbols) {
        DecimalFormat formatter = new DecimalFormat(pattern, formatSymbols);
        formatter.setRoundingMode(RoundingMode.DOWN);
        return formatter;
    }

    public String format(long number) {
        if (!needsSuffix(number)) {
            return noDecimalFormat.format(number);
        }
        return formatCondensedNumber(number, 0);
    }

    private boolean needsSuffix(long number) {
        return number >= TEN * ONE_THOUSAND;
    }

    private String formatCondensedNumber(double number, int suffixIndex) {
        if (number < ONE_THOUSAND) {
            return formatDecimalNumber(number, suffixes[suffixIndex]);
        }
        return formatCondensedNumber(number / ONE_THOUSAND, suffixIndex + 1);
    }

    private String formatDecimalNumber(double number, String suffix) {
        if (number >= ONE_HUNDRED) {
            return noDecimalFormat.format(number) + suffix;
        }

        if (number >= TEN) {
            return singleDecimalFormat.format(number) + suffix;
        }

        return doubleDecimalFormat.format(number) + suffix;
    }

}
