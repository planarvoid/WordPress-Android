package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

import java.util.List;
import java.util.UUID;

@AutoValue
public abstract class ApiVideoAd {
    @JsonCreator
    public static ApiVideoAd create(@JsonProperty("urn") Urn adUrn,
                                    @JsonProperty("expiry_in_minutes") int expiryInMins,
                                    @JsonProperty("duration") long duration,
                                    @JsonProperty("title") Optional<String> title,
                                    @JsonProperty("cta_button_text") Optional<String> ctaButtonText,
                                    @JsonProperty("clickthrough_url") String clickthroughUrl,
                                    @JsonProperty("display_properties") ApiDisplayProperties displayProperties,
                                    @JsonProperty("video_sources") List<ApiVideoSource> videoSources,
                                    @JsonProperty("video_tracking") ApiAdTracking videoTracking,
                                    @JsonProperty("skippable") boolean skippable) {
        return new AutoValue_ApiVideoAd(adUrn,
                                        expiryInMins,
                                        duration,
                                        UUID.randomUUID().toString(),
                                        title,
                                        ctaButtonText,
                                        clickthroughUrl,
                                        displayProperties,
                                        videoSources,
                                        videoTracking,
                                        skippable);
    }

    public abstract Urn getAdUrn();

    public abstract int getExpiryInMins();

    public abstract long getDuration();

    public abstract String getUuid();

    public abstract Optional<String> getTitle();

    public abstract Optional<String> getCallToActionButtonText();

    public abstract String getClickThroughUrl();

    public abstract ApiDisplayProperties getDisplayProperties();

    public abstract List<ApiVideoSource> getVideoSources();

    public abstract ApiAdTracking getVideoTracking();

    public abstract boolean isSkippable();

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
