package com.soundcloud.android.ads;

import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;

import java.util.Arrays;
import java.util.Collections;

public class AdFixtures {

    public static InterstitialAd getInterstitialAd(Urn monetizableUrn) {
        final InterstitialAd interstitial = InterstitialAd.create(getApiInterstitial(), monetizableUrn);
        interstitial.setMonetizableTitle("dubstep anthem");
        interstitial.setMonetizableCreator("squirlex");
        return interstitial;
    }

    public static LeaveBehindAd getLeaveBehindAd(Urn audioAdUrn) {
        return LeaveBehindAd.create(getApiLeaveBehind(), audioAdUrn);
    }

    public static LeaveBehindAd getLeaveBehindAdWithDisplayMetaData(Urn audioAdUrn) {
        final LeaveBehindAd leaveBehind = LeaveBehindAd.create(getApiLeaveBehind(), audioAdUrn);
        leaveBehind.setMetaAdCompleted();
        return leaveBehind;
    }

    public static AudioAd getAudioAd(Urn monetizableUrn) {
        return AudioAd.create(getApiAudioAd(), monetizableUrn);
    }

    public static VideoAd getVideoAd(Urn monetizableUrn) {
        return VideoAd.create(getApiVideoAd(), monetizableUrn);
    }

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
                Arrays.asList("comp_impression1", "comp_impression2"),
                Arrays.asList("comp_click1", "comp_click2"),
                getApiDisplayProperties()
        );
    }

    public static ApiInterstitial getApiInterstitial() {
        return new ApiInterstitial(
                "adswizz:35",
                "http://image.visualad.com",
                "http://clickthrough.visualad.com",
                Arrays.asList("interstitial_impression1", "intersitial_impression2"),
                Arrays.asList("interstitial_click1", "interstitial_click2")
        );
    }

    public static ApiLeaveBehind getApiLeaveBehind() {
        return new ApiLeaveBehind(
                "adswizz:35",
                "http://image.visualad.com",
                "http://clickthrough.visualad.com",
                Arrays.asList("leave_impression1", "leave_impression2"),
                Arrays.asList("leave_click1", "leave_click2")
        );
    }

    public static ApiAudioAd getApiAudioAd() {
        return new ApiAudioAd(
                "adswizz:ads:869",
                ModelFixtures.create(ApiTrack.class),
                getApiCompanionAd(),
                getApiLeaveBehind(),
                Arrays.asList("audio_impression1", "audio_impression2"),
                Arrays.asList("audio_finish1", "audio_finish2"),
                Arrays.asList("audio_skip1", "audio_skip2")
        );
    }

    public static ApiAudioAd getApiAudioAdWithoutLeaveBehind() {
        return new ApiAudioAd(
                "adswizz:ads:869",
                ModelFixtures.create(ApiTrack.class),
                getApiCompanionAd(),
                null,
                Arrays.asList("audio_impression1", "audio_impression2"),
                Arrays.asList("audio_finish1", "audio_finish2"),
                Arrays.asList("audio_skip1", "audio_skip2")
        );
    }

    public static ApiVideoSource getApiVideoSource() {
        return ApiVideoSource.create(
                "video/mp4",
                "http://videourl.com/video.mp4",
                2884,
                608,
                1080
        );
    }

    public static ApiVideoTracking getApiVideoAdTracking() {
        return new ApiVideoTracking(
                Arrays.asList("video_impression1", "video_impression2"),
                Arrays.asList("video_skip1", "video_skip2"),
                Arrays.asList("video_start1", "video_start2"),
                Arrays.asList("video_quartile1_1", "video_quartile1_2"),
                Arrays.asList("video_quartile2_1", "video_quartile2_2"),
                Arrays.asList("video_quartile3_1", "video_quartile3_2"),
                Arrays.asList("video_finish1", "video_finish2"),
                Arrays.asList("video_pause1", "video_pause2"),
                Arrays.asList("video_resume1", "video_resume2"),
                Arrays.asList("video_fullscreen1", "video_fullscreen2"),
                Arrays.asList("video_exit_full1", "video_exit_full2")
        );
    }

    public static ApiVideoAd getApiVideoAd() {
        return ApiVideoAd.create(
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