package com.soundcloud.android.ads;

import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;

import java.util.Collections;

public class AdFixtures {
    public static ApiDisplayProperties getApiDisplayProperties() {
        return new ApiDisplayProperties(
                "#111111",
                "#222222",
                "#333333",
                "#444444",
                "#555555",
                "#666666"
        );
    }

    public static ApiCompanionAd getApiCompanionAd() {
        return new ApiCompanionAd(
                "ad:urn:746",
                "http://image.visualad.com",
                "http://clickthrough.visualad.com",
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                getApiDisplayProperties()
        );
    }

    public static ApiInterstitial getApiInterstitial() {
        return new ApiInterstitial(
                "adswizz:35",
                "http://image.visualad.com",
                "http://clickthrough.visualad.com",
                Collections.<String>emptyList(),
                Collections.<String>emptyList()
        );
    }

    public static ApiLeaveBehind getApiLeaveBehind() {
        return new ApiLeaveBehind(
                "adswizz:35",
                "http://image.visualad.com",
                "http://clickthrough.visualad.com",
                Collections.<String>emptyList(),
                Collections.<String>emptyList()
        );
    }

    public static ApiAudioAd getApiAudioAd() {
        return new ApiAudioAd(
                "adswizz:ads:869",
                ModelFixtures.create(ApiTrack.class),
                getApiCompanionAd(),
                getApiLeaveBehind(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList()
        );
    }

    public static ApiVideoSource getApiVideoSource() {
        return new ApiVideoSource(
                "video/mp4",
                "http://videourl.com/video.mp4",
                2884,
                608,
                1080
        );
    }

    public static ApiVideoTracking getApiVideoAdTracking() {
        return new ApiVideoTracking(
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                Collections.<String>emptyList()
        );
    }

    public static ApiVideoAd getApiVideoAd() {
        return new ApiVideoAd(
                "dfp:ads:905",
                Collections.singletonList(getApiVideoSource()),
                getApiVideoAdTracking(),
                getApiCompanionAd()
        );
    }

    public static ApiAdsForTrack interstitialAdsForTrack(){
        return new ApiAdsForTrack(newArrayList(
                ApiAdWrapper.create(getApiInterstitial()))
        );
    }

    public static ApiAdsForTrack audioAdsForTrack(){
        return new ApiAdsForTrack(newArrayList(
                ApiAdWrapper.create(getApiAudioAd()))
        );
    }

    public static ApiAdsForTrack videoAdsForTrack(){
        return new ApiAdsForTrack(newArrayList(
                ApiAdWrapper.create(getApiVideoAd()))
        );
    }

    public static ApiAdsForTrack fullAdsForTrack(){
        return new ApiAdsForTrack(newArrayList(
                ApiAdWrapper.create(getApiAudioAd()),
                ApiAdWrapper.create(getApiVideoAd()),
                ApiAdWrapper.create(getApiInterstitial()))
        );
    }
}