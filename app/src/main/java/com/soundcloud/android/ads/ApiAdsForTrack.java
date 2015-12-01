package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.VisibleForTesting;

import java.util.List;
import java.util.Map;

class ApiAdsForTrack extends ModelCollection<ApiAdWrapper> {

    public ApiAdsForTrack(@JsonProperty("collection") List<ApiAdWrapper> collection,
                          @JsonProperty("_links") Map<String, Link> links,
                          @JsonProperty("query_urn") String queryUrn) {
        super(collection, links, queryUrn);
    }

    @VisibleForTesting
    public ApiAdsForTrack(List<ApiAdWrapper> collection) {
        super(collection);
    }

    public Optional<ApiInterstitial> interstitialAd() {
        for (ApiAdWrapper adWrapper : this) {
            if (adWrapper.getInterstitial().isPresent()) {
                return adWrapper.getInterstitial();
            }
        }
        return Optional.absent();
    }

    public Optional<ApiAudioAd> audioAd() {
        for (ApiAdWrapper adWrapper : this) {
            if (adWrapper.getAudioAd().isPresent()) {
                return adWrapper.getAudioAd();
            }
        }
        return Optional.absent();
    }

    public Optional<ApiVideoAd> videoAd() {
        for (ApiAdWrapper adWrapper : this) {
            if (adWrapper.getVideoAd().isPresent()) {
                return adWrapper.getVideoAd();
            }
        }
        return Optional.absent();
    }

    public String contentString() {
        final StringBuilder msg = new StringBuilder(100);
        final Optional<ApiAudioAd> audioAd = audioAd();
        final Optional<ApiVideoAd> videoAd = videoAd();
        final Optional<ApiInterstitial> interstitial = interstitialAd();

        if (audioAd.isPresent()) {
            msg.append("audio ad, ");
            if (audioAd.get().hasApiLeaveBehind()) {
                msg.append("leave behind, ");
            }
        }
        if (videoAd.isPresent()) {
            msg.append("video ad, ");
        }
        if (interstitial.isPresent()) {
            msg.append("interstitial");
        }

        return msg.toString();
    }
}
