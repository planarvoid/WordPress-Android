package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;

import java.util.List;

@AutoValue
public abstract class ApiVideoAd {
    @JsonCreator
    public static ApiVideoAd create(@JsonProperty("urn") Urn adUrn,
                                    @JsonProperty("clickthrough_url") String clickthroughUrl,
                                    @JsonProperty("display_properties") ApiDisplayProperties displayProperties,
                                    @JsonProperty("video_sources") List<ApiVideoSource> videoSources,
                                    @JsonProperty("video_tracking") ApiVideoTracking videoTracking) {
        return new AutoValue_ApiVideoAd(adUrn,
                                        clickthroughUrl,
                                        displayProperties,
                                        videoSources,
                                        videoTracking);
    }

    public abstract Urn getAdUrn();

    public abstract String getClickThroughUrl();

    public abstract ApiDisplayProperties getDisplayProperties();

    public abstract List<ApiVideoSource> getVideoSources();

    public abstract ApiVideoTracking getVideoTracking();

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("urn", getAdUrn())
                .add("clickthroughUrl", getClickThroughUrl())
                .add("displayProperties", getDisplayProperties())
                .add("videoSources", getVideoSources())
                .toString();
    }
}
