package com.soundcloud.android.ads;

import com.soundcloud.android.api.model.ModelCollection;

import android.support.annotation.VisibleForTesting;

import java.util.List;

class ApiAdsForTrack extends ModelCollection<ApiAdWrapper> {

    public ApiAdsForTrack() {
        // for deserialization
    }

    @VisibleForTesting
    public ApiAdsForTrack(List<ApiAdWrapper> collection) {
        super(collection);
    }

    public boolean hasInterstitialAd() {
        return interstitialAd() != null;
    }

    public ApiInterstitial interstitialAd() {
        for (ApiAdWrapper adWrapper : this){
            if (adWrapper.hasInterstitialAd()){
                return adWrapper.interstitialAd();
            }
        }
        return null;
    }

    public boolean hasAudioAd() {
        return audioAd() != null;
    }

    public ApiAudioAd audioAd() {
        for (ApiAdWrapper adWrapper : this){
            if (adWrapper.hasAudioAd()){
                return adWrapper.audioAd();
            }
        }
        return null;
    }
}
