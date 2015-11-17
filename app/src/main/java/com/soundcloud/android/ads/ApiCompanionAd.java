package com.soundcloud.android.ads;

import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.java.optional.Optional;

import java.util.List;

class ApiCompanionAd extends ApiBaseAdVisual {

    public final ApiDisplayProperties displayProperties;
    public final Optional<String> ctaButtonText;

    @JsonCreator
    public ApiCompanionAd(@JsonProperty("urn") String urn,
                          @JsonProperty("image_url") String imageUrl,
                          @JsonProperty("clickthrough_url") String clickthroughUrl,
                          @JsonProperty("tracking_impression_urls") List<String> trackingImpressionUrls,
                          @JsonProperty("tracking_click_urls") List<String> trackingClickUrls,
                          @JsonProperty("cta_button_text") @Nullable String ctaButtonText,
                          @JsonProperty("display_properties") ApiDisplayProperties displayProperties) {
        super(urn, imageUrl, clickthroughUrl, trackingImpressionUrls, trackingClickUrls);
        this.displayProperties = displayProperties;
        this.ctaButtonText = Optional.fromNullable(ctaButtonText);
    }

}
