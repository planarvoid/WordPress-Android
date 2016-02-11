package com.soundcloud.android.ads;

import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    public static AudioAd getAudioAdWithCustomCTA(String ctaText, Urn monetizableUrn) {
        return AudioAd.create(
            getApiAudioAdWithCompanion(getApiCompanionAdWithCustomCTA(ctaText)),
            monetizableUrn
        );
    }

    public static VideoAd getVideoAd(Urn monetizableUrn) {
        return VideoAd.create(getApiVideoAd(), monetizableUrn);
    }

    public static VideoAd getVideoAd(Urn monetizableUrn, ApiVideoSource videoSource) {
        return VideoAd.create(getApiVideoAd(videoSource), monetizableUrn);
    }

    public static VideoAd getVideoAd(Urn monetizableUrn, List<ApiVideoSource> videoSources) {
        return VideoAd.create(getApiVideoAd(videoSources), monetizableUrn);
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

    static ApiCompanionAd getApiCompanionAd() {
        final String ctaText = null;
        return getApiCompanionAdWithCustomCTA(ctaText);
    }

    static ApiCompanionAd getApiCompanionAdWithCustomCTA(String ctaText) {
        return new ApiCompanionAd(
                Urn.forAd("dfp", "746"),
                "http://image.visualad.com",
                "http://clickthrough.visualad.com",
                Arrays.asList("comp_impression1", "comp_impression2"),
                Arrays.asList("comp_click1", "comp_click2"),
                ctaText,
                getApiDisplayProperties()
        );
    }

    static ApiInterstitial getApiInterstitial() {
        return new ApiInterstitial(
                Urn.forAd("dfp", "35"),
                "http://image.visualad.com",
                "http://clickthrough.visualad.com",
                Arrays.asList("interstitial_impression1", "intersitial_impression2"),
                Arrays.asList("interstitial_click1", "interstitial_click2")
        );
    }

    static ApiLeaveBehind getApiLeaveBehind() {
        return new ApiLeaveBehind(
                Urn.forAd("dfp", "35"),
                "http://image.visualad.com",
                "http://clickthrough.visualad.com",
                Arrays.asList("leave_impression1", "leave_impression2"),
                Arrays.asList("leave_click1", "leave_click2")
        );
    }

    static ApiAudioAd getApiAudioAd() {
        return getApiAudioAdWithCompanion(getApiCompanionAd());
    }

    static ApiAudioAd getApiAudioAdWithCompanion(ApiCompanionAd companion) {
        return new ApiAudioAd(
                Urn.forAd("dfp", "869"),
                ModelFixtures.create(ApiTrack.class),
                companion,
                getApiLeaveBehind(),
                Arrays.asList("audio_impression1", "audio_impression2"),
                Arrays.asList("audio_finish1", "audio_finish2"),
                Arrays.asList("audio_skip1", "audio_skip2")
        );
    }

    static ApiAudioAd getApiAudioAdWithoutLeaveBehind() {
        return new ApiAudioAd(
                Urn.forAd("dfp", "869"),
                ModelFixtures.create(ApiTrack.class),
                getApiCompanionAd(),
                null,
                Arrays.asList("audio_impression1", "audio_impression2"),
                Arrays.asList("audio_finish1", "audio_finish2"),
                Arrays.asList("audio_skip1", "audio_skip2")
        );
    }

    public static ApiVideoSource getApiVideoSource(int width, int height, int bitRate) {
        return getApiVideoSource(width, height, "video/mp4", bitRate);
    }

    public static ApiVideoSource getApiVideoSource(int width, int height, String type, int bitRate) {
        return ApiVideoSource.create(
                type,
                "http://videourl.com/video.mp4",
                bitRate,
                width,
                height
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
        return getApiVideoAd(getApiVideoSource(608, 1080, 2884));
    }

    public static ApiVideoAd getApiVideoAd(ApiVideoSource apiVideoSource) {
        return getApiVideoAd(Collections.singletonList(apiVideoSource)) ;
    }

    public static ApiVideoAd getApiVideoAd(List<ApiVideoSource> apiVideoSources) {
        return ApiVideoAd.create(
                Urn.forAd("dfp", "905"),
                apiVideoSources,
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