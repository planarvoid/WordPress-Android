package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ApiAdWrapper {

    @Nullable private final ApiAudioAd apiAudioAd;
    @Nullable private final ApiInterstitial interstitial;

    public ApiAdWrapper(ApiAudioAd apiAudioAd) {
        this(apiAudioAd, null);
    }

    public ApiAdWrapper(ApiInterstitial apiLeaveBehind) {
        this(null, apiLeaveBehind);
    }

    @JsonCreator
    public ApiAdWrapper(@JsonProperty("audio_ad") @Nullable ApiAudioAd apiAudioAd,
                          @JsonProperty("interstitial") @Nullable ApiInterstitial interstitial) {
        this.apiAudioAd = apiAudioAd;
        this.interstitial = interstitial;
    }

    public boolean hasAudioAd() {
        return apiAudioAd != null;
    }

    public boolean hasInterstitialAd() {
        return interstitial != null;
    }

    @NotNull
    public ApiAudioAd audioAd() {
        if (apiAudioAd == null) {
            throw new IllegalStateException("Audio ad is not present");
        }
        return apiAudioAd;
    }

    @NotNull
    public ApiInterstitial interstitialAd() {
        if (interstitial == null) {
            throw new IllegalStateException("Interstitial ad is not present");
        }
        return interstitial;
    }

}
