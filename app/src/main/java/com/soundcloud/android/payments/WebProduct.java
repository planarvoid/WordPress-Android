package com.soundcloud.android.payments;

import auto.parcel.AutoParcel;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import android.os.Parcelable;

@AutoParcel
public abstract class WebProduct implements Parcelable {
    @JsonCreator
    static WebProduct create(
            @JsonProperty("plan_id") String planId,
            @JsonProperty("package_urn") String packageUrn,
            @JsonProperty("price") String price,
            @JsonProperty("trial_days") int trialDays,
            @JsonProperty("start_date") String startDate,
            @JsonProperty("expiry_date") String expiryDate
    ) {
        return new AutoParcel_WebProduct(
                planId,
                packageUrn,
                price,
                trialDays,
                startDate,
                expiryDate
        );
    }

    public abstract String getPlanId();
    public abstract String getPackageUrn();
    public abstract String getPrice();
    public abstract int getTrialDays();
    public abstract String getStartDate();
    public abstract String getExpiryDate();
}
