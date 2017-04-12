package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

@AutoValue
abstract class ApiAdWrapper {
    @JsonCreator
    public static ApiAdWrapper create(@JsonProperty("audio_ad") @Nullable AudioAd.ApiModel audioAd,
                                      @JsonProperty("video") @Nullable VideoAd.ApiModel videoAd,
                                      @JsonProperty("interstitial") @Nullable ApiInterstitial interstitial,
                                      @JsonProperty("app_install") @Nullable AppInstallAd.ApiModel appInstall) {
        return new AutoValue_ApiAdWrapper(
                Optional.fromNullable(audioAd),
                Optional.fromNullable(videoAd),
                Optional.fromNullable(interstitial),
                Optional.fromNullable(appInstall)
        );
    }

    @VisibleForTesting
    public static ApiAdWrapper create(AudioAd.ApiModel audioAd) {
        return create(audioAd, null, null, null);
    }

    @VisibleForTesting
    public static ApiAdWrapper create(VideoAd.ApiModel videoAd) {
        return create(null, videoAd, null, null);
    }

    @VisibleForTesting
    public static ApiAdWrapper create(ApiInterstitial interstitial) {
        return create(null, null, interstitial, null);
    }

    public static ApiAdWrapper create(AppInstallAd.ApiModel appInstall) {
        return create(null, null, null, appInstall);
    }

    public abstract Optional<AudioAd.ApiModel> getAudioAd();

    public abstract Optional<VideoAd.ApiModel> getVideoAd();

    public abstract Optional<ApiInterstitial> getInterstitial();

    public abstract Optional<AppInstallAd.ApiModel> getAppInstall();
}
