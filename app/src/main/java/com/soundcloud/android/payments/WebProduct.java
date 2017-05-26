package com.soundcloud.android.payments;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;

import android.os.Parcelable;

@AutoValue
public abstract class WebProduct implements Parcelable {

    @JsonCreator
    static WebProduct create(
            @JsonProperty("plan_id") String planId,
            @JsonProperty("package_urn") String packageUrn,
            @JsonProperty("price_data") WebPrice priceData,
            @JsonProperty("discount_price_data") WebPrice discountPriceData,
            @JsonProperty("trial_days") int trialDays,
            @JsonProperty("promo_days") int promoDays,
            @JsonProperty("promo_price_data") WebPrice promoPriceData,
            @JsonProperty("prorated_price_data") WebPrice proratedPriceData,
            @JsonProperty("start_date") String startDate,
            @JsonProperty("expiry_date") String expiryDate
    ) {
        return new AutoValue_WebProduct(
                planId,
                packageUrn,
                priceData,
                Optional.fromNullable(discountPriceData),
                trialDays,
                promoDays,
                Optional.fromNullable(promoPriceData),
                Optional.fromNullable(proratedPriceData),
                startDate,
                expiryDate
        );
    }

    public abstract String getPlanId();

    public abstract String getPackageUrn();

    public abstract WebPrice getPriceData();

    public abstract Optional<WebPrice> getDiscountPriceData();

    public abstract int getTrialDays();

    public abstract int getPromoDays();

    public abstract Optional<WebPrice> getPromoPriceData();

    public boolean hasPromo() {
        return getPromoDays() > 0;
    }

    public abstract Optional<WebPrice> getProratedPriceData();

    public abstract String getStartDate();

    public abstract String getExpiryDate();

}
