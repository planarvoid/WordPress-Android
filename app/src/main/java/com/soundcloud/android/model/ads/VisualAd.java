package com.soundcloud.android.model.ads;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.AdUrn;
import com.soundcloud.android.model.Urn;

public class VisualAd {

    private AdUrn urn;
    private String imageUrl;
    private String clickthroughUrl;
    private String trackingImpressionUrl;
    private String trackingClickUrl;

    public AdUrn getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = (AdUrn) Urn.parse(urn);
    }

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
