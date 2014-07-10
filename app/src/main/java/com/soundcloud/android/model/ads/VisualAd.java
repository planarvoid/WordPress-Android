package com.soundcloud.android.model.ads;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VisualAd {

    private String imageUrl;
    private String clickthroughUrl;
    private String trackingImpressionUrl;
    private String trackingClickUrl;

    public String getImageUrl() {
        return imageUrl;
    }

    @JsonProperty("image_url")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getClickthroughUrl() {
        return clickthroughUrl;
    }

    @JsonProperty("clickthrough_url")
    public void setClickthroughUrl(String clickthroughUrl) {
        this.clickthroughUrl = clickthroughUrl;
    }

    public String getTrackingImpressionUrl() {
        return trackingImpressionUrl;
    }

    @JsonProperty("tracking_impression_url")
    public void setTrackingImpressionUrl(String trackingImpressionUrl) {
        this.trackingImpressionUrl = trackingImpressionUrl;
    }

    public String getTrackingClickUrl() {
        return trackingClickUrl;
    }

    @JsonProperty("tracking_click_url")
    public void setTrackingClickUrl(String trackingClickUrl) {
        this.trackingClickUrl = trackingClickUrl;
    }

}
