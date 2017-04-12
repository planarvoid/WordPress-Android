package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.List;

@AutoValue
abstract class ApiCompanionAd extends ApiBaseAdVisual {
    @JsonCreator
    static ApiCompanionAd create(@JsonProperty("adUrn") Urn urn,
                                 @JsonProperty("image_url") String imageUrl,
                                 @JsonProperty("clickthrough_url") String clickthroughUrl,
                                 @JsonProperty("tracking_impression_urls") List<String> trackingImpressionUrls,
                                 @JsonProperty("tracking_click_urls") List<String> trackingClickUrls,
                                 @JsonProperty("cta_button_text") Optional<String> ctaButtonText,
                                 @JsonProperty("display_properties") ApiDisplayProperties displayProperties) {
       return new AutoValue_ApiCompanionAd(urn, imageUrl, clickthroughUrl, trackingImpressionUrls,
                                           trackingClickUrls, displayProperties, ctaButtonText);
    }

    public abstract ApiDisplayProperties displayProperties();
    public abstract Optional<String> ctaButtonText();
}
