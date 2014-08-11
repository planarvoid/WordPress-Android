package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.propeller.PropertySet;

import android.net.Uri;

import java.util.List;

public class AudioAd implements PropertySetSource {

    private final String urn;
    private final ApiTrack apiTrack;

    private final VisualAd visualAd;

    private final List<String> trackingImpressionUrls;

    private final List<String> trackingFinishUrls;
    private final List<String> trackingSkipUrls;
    @JsonCreator
    public AudioAd(@JsonProperty("urn") String urn,
                   @JsonProperty("track") ApiTrack apiTrack,
                   @JsonProperty("_embedded") RelatedResources relatedResources,
                   @JsonProperty("tracking_impression_urls") List<String> trackingImpressionUrls,
                   @JsonProperty("tracking_finish_urls") List<String> trackingFinishUrls,
                   @JsonProperty("tracking_skip_urls") List<String> trackingSkipUrls) {
        this(urn, apiTrack, relatedResources.visualAd, trackingImpressionUrls, trackingFinishUrls, trackingSkipUrls);
    }

    @VisibleForTesting
    public AudioAd(String urn, ApiTrack apiTrack, VisualAd visualAd, List<String> trackingImpressionUrls,
                   List<String> trackingFinishUrls, List<String> trackingSkipUrls) {
        this.urn = urn;
        this.apiTrack = apiTrack;
        this.visualAd = visualAd;
        this.trackingImpressionUrls = trackingImpressionUrls;
        this.trackingFinishUrls = trackingFinishUrls;
        this.trackingSkipUrls = trackingSkipUrls;
    }

    public String getUrn() {
        return urn;
    }

    public ApiTrack getApiTrack() {
        return apiTrack;
    }

    public VisualAd getVisualAd() {
        return visualAd;
    }

    public List<String> getTrackingImpressionUrls() {
        return trackingImpressionUrls;
    }

    public List<String> getTrackingFinishUrls() {
        return trackingFinishUrls;
    }

    public List<String> getTrackingSkipUrls() {
        return trackingSkipUrls;
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
        return PropertySet.create(8)
                .put(AdProperty.ARTWORK, Uri.parse(visualAd.getImageUrl()))
                .put(AdProperty.CLICK_THROUGH_LINK, Uri.parse(visualAd.getClickthroughUrl()))
                .put(AdProperty.DEFAULT_TEXT_COLOR, visualAd.getDisplayProperties().getDefaultTextColor())
                .put(AdProperty.DEFAULT_BACKGROUND_COLOR, visualAd.getDisplayProperties().getDefaultBackgroundColor())
                .put(AdProperty.PRESSED_TEXT_COLOR, visualAd.getDisplayProperties().getPressedTextColor())
                .put(AdProperty.PRESSED_BACKGROUND_COLOR, visualAd.getDisplayProperties().getPressedBackgroundColor())
                .put(AdProperty.FOCUSED_TEXT_COLOR, visualAd.getDisplayProperties().getFocusedTextColor())
                .put(AdProperty.FOCUSED_BACKGROUND_COLOR, visualAd.getDisplayProperties().getFocusedBackgroundColor());
    }

}
