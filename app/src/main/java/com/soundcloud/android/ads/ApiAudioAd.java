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
    private final List<ApiAudioAdSource> audioSources;
    private final boolean skippable;

    private final ApiCompanionAd visualAd;
    private final ApiLeaveBehind leaveBehind;

    private final ApiAdTracking apiAdTracking;

    @JsonCreator
    public ApiAudioAd(@JsonProperty("urn") Urn urn,
                      @JsonProperty("track") ApiTrack apiTrack,
                      @JsonProperty("skippable") boolean skippable,
                      @JsonProperty("_embedded") RelatedResources relatedResources,
                      @JsonProperty("audio_sources") List<ApiAudioAdSource> audioSources,
                      @JsonProperty("audio_tracking") ApiAdTracking apiAdTracking) {
        this.urn = urn;
        this.apiTrack = apiTrack;
        this.audioSources = audioSources;
        this.skippable = skippable;
        this.visualAd = relatedResources.visualAd;
        this.leaveBehind = relatedResources.apiLeaveBehind;
        this.apiAdTracking = apiAdTracking;
    }

    public Urn getUrn() {
        return urn;
    }

    public ApiTrack getApiTrack() {
        return apiTrack;
    }

    public List<ApiAudioAdSource> getAudioSources() {
        return audioSources;
    }

    public boolean isSkippable() {
        return skippable;
    }

    public ApiAdTracking getApiAdTracking() {
        return apiAdTracking;
    }

    public boolean hasApiLeaveBehind() {
        return leaveBehind != null;
    }

    public boolean hasCompanion() {
        return visualAd != null;
    }

    @Nullable
    public ApiLeaveBehind getLeaveBehind() {
        return leaveBehind;
    }

    @VisibleForTesting
    @Nullable
    public ApiCompanionAd getCompanion() {
        return visualAd;
    }

    static class RelatedResources {

        private final ApiCompanionAd visualAd;
        private final ApiLeaveBehind apiLeaveBehind;

        @JsonCreator
        RelatedResources(@JsonProperty("visual_ad") ApiCompanionAd visualAd,
                         @JsonProperty("leave_behind") ApiLeaveBehind apiLeaveBehind) {
            this.visualAd = visualAd;
            this.apiLeaveBehind = apiLeaveBehind;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                          .add("urn", urn)
                          .add("apiTrack", apiTrack)
                          .add("visualAd", visualAd)
                          .add("leaveBehind", leaveBehind)
                          .toString();
    }
}
