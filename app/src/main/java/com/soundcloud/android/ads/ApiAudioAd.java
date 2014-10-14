package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;

import java.util.List;

public class ApiAudioAd implements PropertySetSource {

    private final String urn;
    private final ApiTrack apiTrack;

    private final ApiVisualAdWithButton visualAd;

    private final ApiLeaveBehind apiLeaveBehind;
    private final List<String> trackingImpressionUrls;

    private final List<String> trackingFinishUrls;
    private final List<String> trackingSkipUrls;
    @JsonCreator
    public ApiAudioAd(@JsonProperty("urn") String urn,
                      @JsonProperty("track") ApiTrack apiTrack,
                      @JsonProperty("_embedded") RelatedResources relatedResources,
                      @JsonProperty("tracking_impression_urls") List<String> trackingImpressionUrls,
                      @JsonProperty("tracking_finish_urls") List<String> trackingFinishUrls,
                      @JsonProperty("tracking_skip_urls") List<String> trackingSkipUrls) {
        this(urn, apiTrack, relatedResources.visualAd, relatedResources.apiLeaveBehind, trackingImpressionUrls, trackingFinishUrls, trackingSkipUrls);
    }

    @VisibleForTesting
    public ApiAudioAd(String urn, ApiTrack apiTrack, ApiVisualAdWithButton visualAd, ApiLeaveBehind apiLeaveBehind, List<String> trackingImpressionUrls,
                      List<String> trackingFinishUrls, List<String> trackingSkipUrls) {
        this.urn = urn;
        this.apiTrack = apiTrack;
        this.visualAd = visualAd;
        this.apiLeaveBehind = apiLeaveBehind;
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

    public ApiVisualAd getVisualAd() {
        return visualAd;
    }

    public boolean hasApiLeaveBehind() {
        return apiLeaveBehind != null;
    }

    @Nullable
    public ApiLeaveBehind getApiLeaveBehind() {
        return apiLeaveBehind;
    }

    @VisibleForTesting
    /* package */ List<String> getTrackingImpressionUrls() {
        return trackingImpressionUrls;
    }

    @VisibleForTesting
    /* package */ List<String> getTrackingFinishUrls() {
        return trackingFinishUrls;
    }

    @VisibleForTesting
    /* package */ List<String> getTrackingSkipUrls() {
        return trackingSkipUrls;
    }

    private static class RelatedResources {

        private final ApiVisualAdWithButton visualAd;

        private final ApiLeaveBehind apiLeaveBehind;

        @JsonCreator
        private RelatedResources(@JsonProperty("visual_ad") ApiVisualAdWithButton visualAd, @JsonProperty("leave_behind") ApiLeaveBehind apiLeaveBehind) {
            this.visualAd = visualAd;
            this.apiLeaveBehind = apiLeaveBehind;
        }

    }

    @Override
    public PropertySet toPropertySet() {
        return PropertySet.from(
                AdProperty.AD_URN.bind(urn),
                AdProperty.ARTWORK.bind(Uri.parse(visualAd.getImageUrl())),
                AdProperty.CLICK_THROUGH_LINK.bind(Uri.parse(visualAd.getClickthroughUrl())),
                AdProperty.DEFAULT_TEXT_COLOR.bind(visualAd.getDisplayProperties().getDefaultTextColor()),
                AdProperty.DEFAULT_BACKGROUND_COLOR.bind(visualAd.getDisplayProperties().getDefaultBackgroundColor()),
                AdProperty.PRESSED_TEXT_COLOR.bind(visualAd.getDisplayProperties().getPressedTextColor()),
                AdProperty.PRESSED_BACKGROUND_COLOR.bind(visualAd.getDisplayProperties().getPressedBackgroundColor()),
                AdProperty.FOCUSED_TEXT_COLOR.bind(visualAd.getDisplayProperties().getFocusedTextColor()),
                AdProperty.FOCUSED_BACKGROUND_COLOR.bind(visualAd.getDisplayProperties().getFocusedBackgroundColor()),
                AdProperty.AUDIO_AD_IMPRESSION_URLS.bind(trackingImpressionUrls),
                AdProperty.AUDIO_AD_FINISH_URLS.bind(trackingFinishUrls),
                AdProperty.AUDIO_AD_CLICKTHROUGH_URLS.bind(visualAd.getTrackingClickUrls()),
                AdProperty.AUDIO_AD_SKIP_URLS.bind(trackingSkipUrls),
                AdProperty.AUDIO_AD_COMPANION_DISPLAY_IMPRESSION_URLS.bind(visualAd.getTrackingImpressionUrls()));
    }

    @Override
    public String toString() {
        return "AudioAd{" +
                "urn='" + urn + '\'' +
                ", apiTrack=" + apiTrack +
                ", visualAd=" + visualAd +
                ", leaveBehind=" + apiLeaveBehind +
                ", trackingImpressionUrls=" + trackingImpressionUrls +
                ", trackingFinishUrls=" + trackingFinishUrls +
                ", trackingSkipUrls=" + trackingSkipUrls +
                '}';
    }
}
