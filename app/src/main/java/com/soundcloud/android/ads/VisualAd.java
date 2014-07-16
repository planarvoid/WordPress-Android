package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VisualAd {

    private String imageUrl;
    private String clickthroughUrl;
    private String trackingImpressionUrl;
    private String trackingClickUrl;

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
