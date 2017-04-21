package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;

@AutoValue
abstract class ApiAdWrapper {
    @JsonCreator
    public static ApiAdWrapper create(@JsonProperty("audio_ad") Optional<AudioAd.ApiModel> audioAd,
                                      @JsonProperty("video") Optional<VideoAd.ApiModel> videoAd,
                                      @JsonProperty("interstitial") Optional<InterstitialAd.ApiModel> interstitial,
                                      @JsonProperty("app_install") Optional<AppInstallAd.ApiModel> appInstall,
                                      @JsonProperty("display") Optional<VisualPrestitialAd.ApiModel> visualPrestitial) {
        return new AutoValue_ApiAdWrapper(audioAd, videoAd, interstitial, appInstall, visualPrestitial);
    }

    @VisibleForTesting
    public static ApiAdWrapper create(AudioAd.ApiModel audio) {
        return create(Optional.of(audio), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent());
    }

    @VisibleForTesting
    public static ApiAdWrapper create(VideoAd.ApiModel video) {
        return create(Optional.absent(), Optional.of(video), Optional.absent(), Optional.absent(), Optional.absent());
    }

    @VisibleForTesting
    public static ApiAdWrapper create(InterstitialAd.ApiModel interstitial) {
        return create(Optional.absent(), Optional.absent(), Optional.of(interstitial), Optional.absent(), Optional.absent());
    }

    @VisibleForTesting
    public static ApiAdWrapper create(AppInstallAd.ApiModel appInstall) {
        return create(Optional.absent(), Optional.absent(), Optional.absent(), Optional.of(appInstall), Optional.absent());
    }

    @VisibleForTesting
    public static ApiAdWrapper create(VisualPrestitialAd.ApiModel visualPrestitial) {
        return create(Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.of(visualPrestitial));
    }

    public abstract Optional<AudioAd.ApiModel> audioAd();

    public abstract Optional<VideoAd.ApiModel> videoAd();

    public abstract Optional<InterstitialAd.ApiModel> interstitial();

    public abstract Optional<AppInstallAd.ApiModel> appInstall();

    public abstract Optional<VisualPrestitialAd.ApiModel> visualPrestitial();
}
