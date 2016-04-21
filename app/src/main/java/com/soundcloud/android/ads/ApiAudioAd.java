package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;
import org.jetbrains.annotations.Nullable;

import android.support.annotation.VisibleForTesting;

import java.util.List;

class ApiAudioAd {

    private final Urn urn;
    private final ApiTrack apiTrack;
    private final boolean skippable;

    private final ApiCompanionAd visualAd;
    private final ApiLeaveBehind leaveBehind;

    public final List<String> trackingImpressionUrls;
    public final List<String> trackingFinishUrls;
    public final List<String> trackingSkipUrls;

    @JsonCreator
    public ApiAudioAd(@JsonProperty("urn") Urn urn,
                      @JsonProperty("track") ApiTrack apiTrack,
                      @JsonProperty("skippable") boolean skippable,
                      @JsonProperty("_embedded") RelatedResources relatedResources,
                      @JsonProperty("tracking_impression_urls") List<String> trackingImpressionUrls,
                      @JsonProperty("tracking_finish_urls") List<String> trackingFinishUrls,
                      @JsonProperty("tracking_skip_urls") List<String> trackingSkipUrls) {
        this(urn, apiTrack, skippable, relatedResources.visualAd, relatedResources.apiLeaveBehind, trackingImpressionUrls, trackingFinishUrls, trackingSkipUrls);
    }

    @VisibleForTesting
    public ApiAudioAd(Urn urn, ApiTrack apiTrack, boolean skippable, ApiCompanionAd visualAd, ApiLeaveBehind leaveBehind, List<String> trackingImpressionUrls,
                      List<String> trackingFinishUrls, List<String> trackingSkipUrls) {
        this.urn = urn;
        this.apiTrack = apiTrack;
        this.skippable = skippable;
        this.visualAd = visualAd;
        this.leaveBehind = leaveBehind;
        this.trackingImpressionUrls = trackingImpressionUrls;
        this.trackingFinishUrls = trackingFinishUrls;
        this.trackingSkipUrls = trackingSkipUrls;
    }

    public Urn getUrn() {
        return urn;
    }

    public ApiTrack getApiTrack() {
        return apiTrack;
    }

    public boolean isSkippable() {
       return skippable;
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

    public boolean isThirdParty() {
        return apiTrack.getUrn().equals(AdConstants.THIRD_PARTY_AD_MAGIC_TRACK_URN);
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
