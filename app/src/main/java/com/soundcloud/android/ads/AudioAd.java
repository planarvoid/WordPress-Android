package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;

public class AudioAd {

    private ApiTrack apiTrack;

    private VisualAd visualAd;

    private String trackingImpressionUrl;
    private String trackingPlayUrl;
    private String trackingFinishUrl;
    private String trackingSkipUrl;

    @JsonCreator
    public AudioAd(@JsonProperty("track") ApiTrack apiTrack,
                   @JsonProperty("_embedded") RelatedResources relatedResources,
                   @JsonProperty("tracking_impression_url") String trackingImpressionUrl,
                   @JsonProperty("tracking_play_url") String trackingPlayUrl,
                   @JsonProperty("tracking_finish_url") String trackingFinishUrl,
                   @JsonProperty("tracking_skip_url") String trackingSkipUrl) {
        this.apiTrack = apiTrack;
        this.visualAd = relatedResources.visualAd;
        this.trackingImpressionUrl = trackingImpressionUrl;
        this.trackingPlayUrl = trackingPlayUrl;
        this.trackingFinishUrl = trackingFinishUrl;
        this.trackingSkipUrl = trackingSkipUrl;
    }

    public ApiTrack getApiTrack() {
        return apiTrack;
    }

    public VisualAd getVisualAd() {
        return visualAd;
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

    private static class RelatedResources {
        private final VisualAd visualAd;

        @JsonCreator
        private RelatedResources(@JsonProperty("visual_ad") VisualAd visualAd) {
            this.visualAd = visualAd;
        }
    }

}
