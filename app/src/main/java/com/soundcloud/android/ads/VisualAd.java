package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class VisualAd {

    private final String imageUrl;
    private final String clickthroughUrl;
    private final List<String> trackingImpressionUrls;
    private final List<String> trackingClickUrls;
    private final DisplayProperties displayProperties;

    @JsonCreator
    public VisualAd(@JsonProperty("image_url") String imageUrl,
                    @JsonProperty("clickthrough_url") String clickthroughUrl,
                    @JsonProperty("tracking_impression_urls") List<String> trackingImpressionUrls,
                    @JsonProperty("tracking_click_urls") List<String> trackingClickUrls,
                    @JsonProperty("display_properties") DisplayProperties displayProperties) {
        this.imageUrl = imageUrl;
        this.clickthroughUrl = clickthroughUrl;
        this.trackingImpressionUrls = trackingImpressionUrls;
        this.trackingClickUrls = trackingClickUrls;
        this.displayProperties = displayProperties;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getClickthroughUrl() {
        return clickthroughUrl;
    }

    public List<String> getTrackingImpressionUrls() {
        return trackingImpressionUrls;
    }

    public List<String> getTrackingClickUrls() {
        return trackingClickUrls;
    }

    public DisplayProperties getDisplayProperties() {
        return displayProperties;
    }
}
