package com.soundcloud.android.payments;

import auto.parcel.AutoParcel;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.java.optional.Optional;

import android.os.Parcelable;
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
            @JsonProperty("start_date") String startDate,
            @JsonProperty("expiry_date") String expiryDate
    ) {
        return new AutoParcel_WebProduct(
                planId,
                packageUrn,
                reformatPrice(price),
                reformatDiscount(discountPrice),
                rawPrice,
                rawCurrency,
                trialDays,
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
    public abstract String getStartDate();
    public abstract String getExpiryDate();

    private static Optional<String> reformatDiscount(@Nullable String discountPrice) {
        return discountPrice == null
                ? Optional.<String>absent()
                : Optional.of(reformatPrice(discountPrice));
    }

    private static String reformatPrice(String price) {
        char first = price.charAt(0);
        if (first == '€') {
            return reformatForLocale(price.charAt(0), price.substring(1, price.length()));
        } else {
            return price;
        }
    }

    private static String reformatForLocale(char symbol, String value) {
        if (isEnglish()) {
            return symbol + value;
        } else {
            return value.replace('.', ',') + symbol;
        }
    }

    private static boolean isEnglish() {
        return Locale.getDefault().getISO3Language().equals(Locale.ENGLISH.getISO3Language());
    }

}
