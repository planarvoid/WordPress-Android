package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

@AutoValue
abstract class ApiAppInstallAd {
    @JsonCreator
    public static ApiAppInstallAd create(@JsonProperty("urn") Urn adUrn,
                                         @JsonProperty("name") String name,
                                         @JsonProperty("cta_button_text") String ctaButtonText,
                                         @JsonProperty("clickthrough_url") String clickthroughUrl,
                                         @JsonProperty("image_url") String imageUrl,
                                         @JsonProperty("rating") float rating,
                                         @JsonProperty("rater_count") int ratersCount,
                                         @JsonProperty("app_install_tracking") ApiAdTracking tracking) {
        return new AutoValue_ApiAppInstallAd(adUrn,
                                             name,
                                             ctaButtonText,
                                             clickthroughUrl,
                                             imageUrl,
                                             rating,
                                             ratersCount,
                                             tracking);
    }

    public abstract Urn getAdUrn();

    public abstract String getName();

    public abstract String getCtaButtonText();

    public abstract String getClickThroughUrl();

    public abstract String getImageUrl();

    public abstract float getRating();

    public abstract int getRatersCount();

    public abstract ApiAdTracking apiAdTracking();
}
