package com.soundcloud.android.ads;

import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;

@AutoValue
abstract class ApiAdWrapper {
    @JsonCreator
    public static ApiAdWrapper create(@JsonProperty("audio_ad") @Nullable ApiAudioAd audioAd,
                                      @JsonProperty("video") @Nullable ApiVideoAd videoAd,
                                      @JsonProperty("interstitial") @Nullable ApiInterstitial interstitial) {
        return new AutoValue_ApiAdWrapper(
                Optional.fromNullable(audioAd),
                Optional.fromNullable(videoAd),
                Optional.fromNullable(interstitial)
        );
    }

    @VisibleForTesting
    public static ApiAdWrapper create(ApiAudioAd audioAd) {
        return create(audioAd, null, null);
    }

    @VisibleForTesting
    public static ApiAdWrapper create(ApiVideoAd videoAd) {
        return create(null, videoAd, null);
    }

    @VisibleForTesting
    public static ApiAdWrapper create(ApiInterstitial interstitial) {
        return create(null, null, interstitial);
    }

    public abstract Optional<ApiAudioAd> getAudioAd();

    public abstract Optional<ApiVideoAd> getVideoAd();

    public abstract Optional<ApiInterstitial> getInterstitial();
}
