package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ApiVisualAd {

    private final String imageUrl;
    private final String clickthroughUrl;
    private final List<String> trackingImpressionUrls;
    private final List<String> trackingClickUrls;

    @JsonCreator
    public ApiVisualAd(@JsonProperty("image_url") String imageUrl,
                       @JsonProperty("clickthrough_url") String clickthroughUrl,
                       @JsonProperty("tracking_impression_urls") List<String> trackingImpressionUrls,
                       @JsonProperty("tracking_click_urls") List<String> trackingClickUrls) {
        this.imageUrl = imageUrl;
        this.clickthroughUrl = clickthroughUrl;
        this.trackingImpressionUrls = trackingImpressionUrls;
        this.trackingClickUrls = trackingClickUrls;
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

    @Override
    public String toString() {
        return "VisualAd{" +
                "imageUrl='" + imageUrl + '\'' +
                ", clickthroughUrl='" + clickthroughUrl + '\'' +
                ", trackingImpressionUrls=" + trackingImpressionUrls +
                ", trackingClickUrls=" + trackingClickUrls +
                '}';
    }
}
