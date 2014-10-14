package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ApiVisualAdWithButton extends ApiVisualAd {

    private final DisplayProperties displayProperties;

    @JsonCreator
    public ApiVisualAdWithButton(@JsonProperty("image_url") String imageUrl,
                                 @JsonProperty("clickthrough_url") String clickthroughUrl,
                                 @JsonProperty("tracking_impression_urls") List<String> trackingImpressionUrls,
                                 @JsonProperty("tracking_click_urls") List<String> trackingClickUrls,
                                 @JsonProperty("display_properties") DisplayProperties displayProperties) {
        super(imageUrl, clickthroughUrl, trackingImpressionUrls, trackingClickUrls);
        this.displayProperties = displayProperties;
    }

    public DisplayProperties getDisplayProperties() {
        return displayProperties;
    }
}
