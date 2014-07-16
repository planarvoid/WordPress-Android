package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VisualAd {

    private final String imageUrl;
    private final String clickthroughUrl;
    private final String trackingImpressionUrl;
    private final String trackingClickUrl;

    public VisualAd(@JsonProperty("image_url") String imageUrl,
                    @JsonProperty("clickthrough_url") String clickthroughUrl,
                    @JsonProperty("tracking_impression_url") String trackingImpressionUrl,
                    @JsonProperty("tracking_click_url") String trackingClickUrl) {
        this.imageUrl = imageUrl;
        this.clickthroughUrl = clickthroughUrl;
        this.trackingImpressionUrl = trackingImpressionUrl;
        this.trackingClickUrl = trackingClickUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getClickthroughUrl() {
        return clickthroughUrl;
    }

    public String getTrackingImpressionUrl() {
        return trackingImpressionUrl;
    }

    public String getTrackingClickUrl() {
        return trackingClickUrl;
    }

}
