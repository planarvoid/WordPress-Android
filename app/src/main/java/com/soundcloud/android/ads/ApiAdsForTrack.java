package com.soundcloud.android.ads;


import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.soundcloud.android.api.model.ModelCollection;

import java.util.List;

public class ApiAdsForTrack extends ModelCollection<ApiAdWrapper> {

    public ApiAdsForTrack() {
        // for deserialization
    }

    @VisibleForTesting
    public ApiAdsForTrack(List<ApiAdWrapper> collection) {
        super(collection);
    }

    public boolean hasInterstitialAd() {
        return Iterables.find(this, new Predicate<ApiAdWrapper>() {
            @Override
            public boolean apply(ApiAdWrapper input) {
                return input.hasInterstitialAd();
            }
        }, null) != null;
    }

    public ApiInterstitial interstitialAd() {
        return Iterables.find(this, new Predicate<ApiAdWrapper>() {
            @Override
            public boolean apply(ApiAdWrapper input) {
                return input.hasInterstitialAd();
            }
        }).interstitialAd();
    }

    public boolean hasAudioAd() {
        return Iterables.find(this, new Predicate<ApiAdWrapper>() {
            @Override
            public boolean apply(ApiAdWrapper input) {
                return input.hasAudioAd();
            }
        }, null) != null;
    }

    public ApiAudioAd audioAd() {
        return Iterables.find(this, new Predicate<ApiAdWrapper>() {
            @Override
            public boolean apply(ApiAdWrapper input) {
                return input.hasAudioAd();
            }
        }).audioAd();
    }
}
