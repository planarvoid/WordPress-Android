package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class LeaveBehind {

    private final String urn;
    private final String imageUrl;
    private final String clickthroughUrl;
    private final List<String> trackingImpressionUrls;
    private final List<String> trackingClickUrls;

    @JsonCreator
    public LeaveBehind(
            @JsonProperty("urn") String urn,
            @JsonProperty("image_url") String imageUrl,
            @JsonProperty("clickthrough_url") String clickthroughUrl,
            @JsonProperty("tracking_impression_urls") List<String> trackingImpressionUrls,
            @JsonProperty("tracking_click_urls") List<String> trackingClickUrls) {
        this.urn = urn;
        this.imageUrl = imageUrl;
        this.clickthroughUrl = clickthroughUrl;
        this.trackingImpressionUrls = trackingImpressionUrls;
        this.trackingClickUrls = trackingClickUrls;
    }

    public String getUrn() {
        return urn;
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
        return "LeaveBehind{" +
                "urn='" + urn + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", clickthroughUrl='" + clickthroughUrl + '\'' +
                ", trackingImpressionUrls=" + trackingImpressionUrls +
                ", trackingClickUrls=" + trackingClickUrls +
                '}';
    }
}
