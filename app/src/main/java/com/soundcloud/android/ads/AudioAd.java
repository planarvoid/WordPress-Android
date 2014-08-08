package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.propeller.PropertySet;

import android.net.Uri;

public class AudioAd implements PropertySetSource {

    private final ApiTrack apiTrack;

    private final VisualAd visualAd;

    private final String trackingImpressionUrl;
    private final String trackingPlayUrl;
    private final String trackingFinishUrl;
    private final String trackingSkipUrl;

    @JsonCreator
    public AudioAd(@JsonProperty("track") ApiTrack apiTrack,
                   @JsonProperty("_embedded") RelatedResources relatedResources,
                   @JsonProperty("tracking_impression_url") String trackingImpressionUrl,
                   @JsonProperty("tracking_play_url") String trackingPlayUrl,
                   @JsonProperty("tracking_finish_url") String trackingFinishUrl,
                   @JsonProperty("tracking_skip_url") String trackingSkipUrl) {
        this(apiTrack, relatedResources.visualAd, trackingImpressionUrl, trackingPlayUrl, trackingFinishUrl, trackingSkipUrl);
    }

    @VisibleForTesting
    public AudioAd(ApiTrack apiTrack, VisualAd visualAd, String trackingImpressionUrl,
                   String trackingPlayUrl, String trackingFinishUrl, String trackingSkipUrl) {
        this.apiTrack = apiTrack;
        this.visualAd = visualAd;
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

    @Override
    public PropertySet toPropertySet() {
        return PropertySet.create(2)
                .put(AdProperty.ARTWORK, Uri.parse(visualAd.getImageUrl()))
                .put(AdProperty.CLICK_THROUGH_LINK, Uri.parse(visualAd.getClickthroughUrl()));
    }

}
