package com.soundcloud.android.ads;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.events.AdsReceived;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.VisibleForTesting;

import java.util.List;
import java.util.Map;

class ApiAdsForTrack extends ModelCollection<ApiAdWrapper> implements AdsCollection {

    public ApiAdsForTrack(@JsonProperty("collection") List<ApiAdWrapper> collection,
                          @JsonProperty("_links") Map<String, Link> links,
                          @JsonProperty("query_urn") String queryUrn) {
        super(collection, links, queryUrn);
    }

    @VisibleForTesting
    public ApiAdsForTrack(List<ApiAdWrapper> collection) {
        super(collection);
    }

    public Optional<InterstitialAd.ApiModel> interstitialAd() {
        for (ApiAdWrapper adWrapper : this) {
            if (adWrapper.getInterstitial().isPresent()) {
                return adWrapper.getInterstitial();
            }
        }
        return Optional.absent();
    }

    public Optional<AudioAd.ApiModel> audioAd() {
        for (ApiAdWrapper adWrapper : this) {
            if (adWrapper.getAudioAd().isPresent()) {
                return adWrapper.getAudioAd();
            }
        }
        return Optional.absent();
    }

    public Optional<VideoAd.ApiModel> videoAd() {
        for (ApiAdWrapper adWrapper : this) {
            if (adWrapper.getVideoAd().isPresent()) {
                return adWrapper.getVideoAd();
            }
        }
        return Optional.absent();
    }

    @Override
    public AdsReceived toAdsReceived() {
        final Optional<AudioAd.ApiModel> audioAd = audioAd();
        final Optional<VideoAd.ApiModel> videoAd = videoAd();
        final Optional<InterstitialAd.ApiModel> interstitial = interstitialAd();
        return AdsReceived.forPlayerAd(videoAd.isPresent() ? videoAd.get().adUrn() : Urn.NOT_SET,
                                       audioAd.isPresent() ? audioAd.get().adUrn() : Urn.NOT_SET,
                                       interstitial.isPresent() ? interstitial.get().adUrn() : Urn.NOT_SET);
    }

    public String contentString() {
        final StringBuilder msg = new StringBuilder(100);
        final Optional<AudioAd.ApiModel> audioAd = audioAd();
        final Optional<VideoAd.ApiModel> videoAd = videoAd();
        final Optional<InterstitialAd.ApiModel> interstitial = interstitialAd();

        if (audioAd.isPresent()) {
            msg.append("audio ad, ");
            if (audioAd.get().leaveBehind().isPresent()) {
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
