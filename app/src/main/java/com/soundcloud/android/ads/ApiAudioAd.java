package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.PropertySetSource;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;
import org.jetbrains.annotations.Nullable;

import android.net.Uri;
import android.support.annotation.VisibleForTesting;

import java.util.List;

class ApiAudioAd implements PropertySetSource {

    private final String urn;
    private final ApiTrack apiTrack;

    private final ApiCompanionAd visualAd;
    private final ApiLeaveBehind leaveBehind;

    public final List<String> trackingImpressionUrls;
    public final List<String> trackingFinishUrls;
    public final List<String> trackingSkipUrls;

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
    public ApiAudioAd(String urn, ApiTrack apiTrack, ApiCompanionAd visualAd, ApiLeaveBehind leaveBehind, List<String> trackingImpressionUrls,
                      List<String> trackingFinishUrls, List<String> trackingSkipUrls) {
        this.urn = urn;
        this.apiTrack = apiTrack;
        this.visualAd = visualAd;
        this.leaveBehind = leaveBehind;
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

    public boolean hasApiLeaveBehind() {
        return leaveBehind != null;
    }

    @Nullable
    public ApiLeaveBehind getLeaveBehind() {
        return leaveBehind;
    }

    @VisibleForTesting
    public ApiCompanionAd getCompanion() {
        return visualAd;
    }

    private static class RelatedResources {

        private final ApiCompanionAd visualAd;
        private final ApiLeaveBehind apiLeaveBehind;

        @JsonCreator
        private RelatedResources(@JsonProperty("visual_ad") ApiCompanionAd visualAd,
                                 @JsonProperty("leave_behind") ApiLeaveBehind apiLeaveBehind) {
            this.visualAd = visualAd;
            this.apiLeaveBehind = apiLeaveBehind;
        }

    }

    @Override
    public PropertySet toPropertySet() {
        return PropertySet.from(
                AdProperty.AUDIO_AD_URN.bind(urn),
                AdProperty.COMPANION_URN.bind(visualAd.urn),
                AdProperty.ARTWORK.bind(Uri.parse(visualAd.imageUrl)),
                AdProperty.CLICK_THROUGH_LINK.bind(Uri.parse(visualAd.clickthroughUrl)),
                AdProperty.DEFAULT_TEXT_COLOR.bind(visualAd.displayProperties.defaultTextColor),
                AdProperty.DEFAULT_BACKGROUND_COLOR.bind(visualAd.displayProperties.defaultBackgroundColor),
                AdProperty.PRESSED_TEXT_COLOR.bind(visualAd.displayProperties.pressedTextColor),
                AdProperty.PRESSED_BACKGROUND_COLOR.bind(visualAd.displayProperties.pressedBackgroundColor),
                AdProperty.FOCUSED_TEXT_COLOR.bind(visualAd.displayProperties.focusedTextColor),
                AdProperty.FOCUSED_BACKGROUND_COLOR.bind(visualAd.displayProperties.focusedBackgroundColor),
                AdProperty.AUDIO_AD_IMPRESSION_URLS.bind(trackingImpressionUrls),
                AdProperty.AUDIO_AD_FINISH_URLS.bind(trackingFinishUrls),
                AdProperty.AUDIO_AD_CLICKTHROUGH_URLS.bind(visualAd.trackingClickUrls),
                AdProperty.AUDIO_AD_SKIP_URLS.bind(trackingSkipUrls),
                AdProperty.AUDIO_AD_COMPANION_DISPLAY_IMPRESSION_URLS.bind(visualAd.trackingImpressionUrls));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("urn", urn)
                .add("apiTrack", apiTrack)
                .add("visualAd", visualAd)
                .add("leaveBehind", leaveBehind)
                .add("trackingImpressionUrls", trackingImpressionUrls)
                .add("trackingFinishUrls", trackingFinishUrls)
                .add("trackingSkipUrls", trackingSkipUrls)
                .toString();
    }

}
