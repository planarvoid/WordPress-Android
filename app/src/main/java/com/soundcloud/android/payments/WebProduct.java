package com.soundcloud.android.payments;

import auto.parcel.AutoParcel;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.java.optional.Optional;

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Locale;

@AutoParcel
public abstract class WebProduct implements Parcelable {
    @JsonCreator
    static WebProduct create(
            @JsonProperty("plan_id") String planId,
            @JsonProperty("package_urn") String packageUrn,
            @JsonProperty("price") String price,
            @JsonProperty("discount_price") String discountPrice,
            @JsonProperty("raw_price") String rawPrice,
            @JsonProperty("raw_currency") String rawCurrency,
            @JsonProperty("trial_days") int trialDays,
            @JsonProperty("promo_days") int promoDays,
            @JsonProperty("promo_price") String promoPrice,
            @JsonProperty("start_date") String startDate,
            @JsonProperty("expiry_date") String expiryDate
    ) {
        return new AutoParcel_WebProduct(
                planId,
                packageUrn,
                reformatPrice(price),
                reformatOptionalPrice(discountPrice),
                rawPrice,
                rawCurrency,
                trialDays,
                promoDays,
                reformatOptionalPrice(promoPrice),
                startDate,
                expiryDate
        );
    }

    public abstract String getPlanId();

    public abstract String getPackageUrn();

    public abstract String getPrice();

    public abstract Optional<String> getDiscountPrice();

    public abstract String getRawPrice();

    public abstract String getRawCurrency();

    public abstract int getTrialDays();

    public abstract int getPromoDays();

    public abstract Optional<String> getPromoPrice();

    public boolean hasPromo() {
        return getPromoDays() > 0;
    }

    public abstract String getStartDate();

    public abstract String getExpiryDate();

    private static Optional<String> reformatOptionalPrice(@Nullable String price) {
        return price == null
               ? Optional.<String>absent()
               : Optional.of(reformatPrice(price));
    }

    private static String reformatPrice(String price) {
        return price.charAt(0) == '€'
               ? reformatForLocale(price.charAt(0), price.substring(1, price.length()))
               : price;
    }

    private static String reformatForLocale(char symbol, String value) {
        if (isLanguage(Locale.ENGLISH)) {
            return symbol + value;
        } else if (isLanguage(Locale.FRENCH)) {
            return reformatValue(value) + " " + symbol;
        } else {
            return reformatValue(value) + symbol;
        }
    }

    @NonNull
    private static String reformatValue(String value) {
        return value.replace('.', ',');
    }

    private static boolean isLanguage(Locale locale) {
        return Locale.getDefault().getISO3Language().equals(locale.getISO3Language());
    }

}
