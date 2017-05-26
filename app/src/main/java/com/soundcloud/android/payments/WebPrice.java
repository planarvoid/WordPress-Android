package com.soundcloud.android.payments;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import android.os.Parcelable;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

@AutoValue
abstract class WebPrice implements Parcelable, Serializable {

    private static final String EURO = "€";
    private static final String DOLLAR = "$";
    private static final String POUND = "£";

    @JsonCreator
    static WebPrice create(
            @JsonProperty("amount") int amount,
            @JsonProperty("currency") String currency
    ) {
        return new AutoValue_WebPrice(
                amount,
                currency
        );
    }

    public abstract int amount();

    public abstract String currency();

    /*
     * Format for display
     */
    public String format() {
        NumberFormat currencyFormat = configureCurrencyFormatter(currencySymbol());
        return currencyFormat.format(decimalAmount());
    }

    private NumberFormat configureCurrencyFormatter(String symbol) {
        final NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
        if (numberFormat instanceof DecimalFormat) {
            DecimalFormat decimalFormat = (DecimalFormat) numberFormat;
            configureSymbol(decimalFormat, symbol);
            if (!hasDecimalPlaces()) {
                decimalFormat.setMinimumFractionDigits(0);
            }
            return decimalFormat;
        } else {
            return numberFormat;
        }
    }

    private void configureSymbol(DecimalFormat decimalFormat, String symbol) {
        DecimalFormatSymbols symbols = decimalFormat.getDecimalFormatSymbols();
        symbols.setCurrencySymbol(symbol);
        decimalFormat.setDecimalFormatSymbols(symbols);
    }

    /*
     * Format for tracking
     */
    String decimalString() {
        return String.format(Locale.US, "%.2f", decimalAmount());
    }

    private float decimalAmount() {
        return amount() / 100.0f;
    }

    private boolean hasDecimalPlaces() {
        return amount() % 100 > 0;
    }

    /*
     * Favour our simple mapping of supported currencies,
     * because symbol resolution varies by platform version & locale
     */
    private String currencySymbol() {
        switch (currency()) {
            case "USD":
            case "CAD":
            case "AUD":
            case "NZD":
                return DOLLAR;
            case "EUR":
                return EURO;
            case "GBP":
                return POUND;
            default:
                return resolveDefaultSymbol();
        }
    }

    private String resolveDefaultSymbol() {
        try {
            return Currency.getInstance(currency()).getSymbol();
        } catch (IllegalArgumentException e) {
            return currency();
        }
    }

}
