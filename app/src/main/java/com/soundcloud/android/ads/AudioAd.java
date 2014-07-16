package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;

public class AudioAd {

    private ApiTrack apiTrack;

    private VisualAd visualAd;

    private String trackingImpressionUrl;
    private String trackingPlayUrl;
    private String trackingFinishUrl;
    private String trackingSkipUrl;

    public ApiTrack getApiTrack() {
        return apiTrack;
    }

    public VisualAd getVisualAd() {
        return visualAd;
    }

    @JsonProperty("track")
    public void setApiTrack(ApiTrack apiTrack) {
        this.apiTrack = apiTrack;
    }

    @JsonProperty("tracking_impression_url")
    public void setTrackingImpressionUrl(String trackingImpressionUrl) {
        this.trackingImpressionUrl = trackingImpressionUrl;
    }

    @JsonProperty("tracking_play_url")
    public void setTrackingPlayUrl(String trackingPlayUrl) {
        this.trackingPlayUrl = trackingPlayUrl;
    }

    @JsonProperty("tracking_finish_url")
    public void setTrackingFinishUrl(String trackingFinishUrl) {
        this.trackingFinishUrl = trackingFinishUrl;
    }

    @JsonProperty("tracking_skip_url")
    public void setTrackingSkipUrl(String trackingSkipUrl) {
        this.trackingSkipUrl = trackingSkipUrl;
    }

    public String getTrackingImpressionUrl() {
        return trackingImpressionUrl;
    }

    public String getTrackingPlayUrl() {
        return trackingPlayUrl;
    }

    public String getTrackingFinishUrl() {
        return trackingFinishUrl;
    }

    public String getTrackingSkipUrl() {
        return trackingSkipUrl;
    }

    @JsonProperty("_embedded")
    public void setRelatedResources(RelatedResources relatedResources) {
        this.visualAd = relatedResources.visualAd;
    }

    private static class RelatedResources {
        private VisualAd visualAd;

        @JsonProperty("visual_ad")
        void setVisualAd(VisualAd visualAd) {
            this.visualAd = visualAd;
        }
    }

}
