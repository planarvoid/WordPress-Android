package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.java.objects.MoreObjects;

import java.util.List;

@AutoValue
public abstract class ApiVideoAd {
    @JsonCreator
    public static ApiVideoAd create(@JsonProperty("urn") String adUrn,
                                    @JsonProperty("video_sources") List<ApiVideoSource> videoSources,
                                    @JsonProperty("video_tracking") ApiVideoTracking videoTracking,
                                    @JsonProperty("visual_ad") ApiCompanionAd visualAd) {
        return new AutoValue_ApiVideoAd(
                adUrn,
                videoSources,
                videoTracking,
                visualAd
        );
    }

    public abstract String getAdUrn();

    public abstract List<ApiVideoSource> getVideoSources();

    public abstract ApiVideoTracking getVideoTracking();

    public abstract ApiCompanionAd getVisualAd();

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("urn", getAdUrn())
                .add("videoSources", getVideoSources())
                .add("visualAd", getVisualAd())
                .toString();
    }
}
