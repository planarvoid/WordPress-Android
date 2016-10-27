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
                                      @JsonProperty("interstitial") @Nullable ApiInterstitial interstitial,
                                      @JsonProperty("app_install") @Nullable ApiAppInstallAd appInstall) {
        return new AutoValue_ApiAdWrapper(
                Optional.fromNullable(audioAd),
                Optional.fromNullable(videoAd),
                Optional.fromNullable(interstitial),
                Optional.fromNullable(appInstall)
        );
    }

    @VisibleForTesting
    public static ApiAdWrapper create(ApiAudioAd audioAd) {
        return create(audioAd, null, null, null);
    }

    @VisibleForTesting
    public static ApiAdWrapper create(ApiVideoAd videoAd) {
        return create(null, videoAd, null, null);
    }

    @VisibleForTesting
    public static ApiAdWrapper create(ApiInterstitial interstitial) {
        return create(null, null, interstitial, null);
    }

    public static ApiAdWrapper create(ApiAppInstallAd appInstall) {
        return create(null, null, null, appInstall);
    }

    public abstract Optional<ApiAudioAd> getAudioAd();

    public abstract Optional<ApiVideoAd> getVideoAd();

    public abstract Optional<ApiInterstitial> getInterstitial();

    public abstract Optional<ApiAppInstallAd> getAppInstall();
}
