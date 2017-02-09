package com.soundcloud.android.ads;

import com.soundcloud.android.ads.ApiAudioAd.RelatedResources;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.soundcloud.java.collections.Lists.newArrayList;

public class AdFixtures {

    private static final boolean NOT_SKIPPABLE = false;
    private static final int VIDEO_BIT_RATE = 1001;
    private static final boolean SKIPPABLE = true;

    public static InterstitialAd getInterstitialAd(Urn monetizableUrn) {
        final InterstitialAd interstitial = InterstitialAd.create(getApiInterstitial(), monetizableUrn);
        interstitial.setMonetizableTitle("dubstep anthem");
        interstitial.setMonetizableCreator("squirlex");
        return interstitial;
    }

    public static LeaveBehindAd getLeaveBehindAd(Urn audioAdUrn) {
        return LeaveBehindAd.create(getApiLeaveBehind(), audioAdUrn);
    }

    static LeaveBehindAd getLeaveBehindAdWithDisplayMetaData(Urn audioAdUrn) {
        final LeaveBehindAd leaveBehind = LeaveBehindAd.create(getApiLeaveBehind(), audioAdUrn);
        leaveBehind.setMetaAdCompleted();
        return leaveBehind;
    }

    public static AudioAd getAudioAd(Urn monetizableUrn) {
        return AudioAd.create(getApiAudioAd(), monetizableUrn);
    }

    public static AudioAd getAudioAdWithCustomCTA(String ctaText, Urn monetizableUrn) {
        return AudioAd.create(
                getApiAudioAdWithCompanion(getApiCompanionAdWithCustomCTA(ctaText), SKIPPABLE),
                monetizableUrn
        );
    }

    public static AudioAd getAudioAdWithCustomClickthrough(String clickthrough, Urn monetizableUrn) {
        return AudioAd.create(
                getApiAudioAdWithCompanion(getApiCompanionAdWithCustomClickthrough(clickthrough), SKIPPABLE),
                monetizableUrn
        );
    }

    public static AudioAd getNonskippableAudioAd(Urn monetizableUrn) {
        return AudioAd.create(getApiAudioAd(NOT_SKIPPABLE), monetizableUrn);
    }

    public static AudioAd getNonClickableAudioAd(Urn monetizableUrn) {
        return AudioAd.create(getNonClickableApiAudioAd(), monetizableUrn);
    }

    public static AudioAd getCompanionlessAudioAd(Urn monetizableUrn) {
        return AudioAd.create(getCompanionlessAudioAd(), monetizableUrn);
    }

    public static VideoAd getVideoAd(long createdAt) {
        return VideoAd.create(getApiVideoAd(), createdAt);
    }

    public static VideoAd getVideoAd(Urn monetizableUrn) {
        return VideoAd.create(getApiVideoAd(), 0, monetizableUrn);
    }

    public static VideoAd getVideoAd(Urn monetizableUrn, ApiVideoSource videoSource) {
        return VideoAd.create(getApiVideoAd(videoSource, SKIPPABLE), 0, monetizableUrn);
    }

    public static VideoAd getVideoAd(Urn monetizableUrn, List<ApiVideoSource> videoSources) {
        return VideoAd.create(getApiVideoAd(videoSources, SKIPPABLE, Optional.absent(), Optional.absent()), 0, monetizableUrn);
    }

    public static VideoAd getNonskippableVideoAd(Urn monetizableUrn) {
        return VideoAd.create(getApiVideoAd(NOT_SKIPPABLE), 0, monetizableUrn);
    }

    private static ApiDisplayProperties getApiDisplayProperties() {
        return new ApiDisplayProperties(
                "#111111",
                "#222222",
                "#333333",
                "#444444",
                "#555555",
                "#666666"
        );
    }

    private static ApiCompanionAd getApiCompanionAd() {
        final String ctaText = null;
        return getApiCompanionAdWithCustomCTA(ctaText);
    }

    private static ApiCompanionAd getApiCompanionAdWithCustomClickthrough(String clickthrough) {
        return new ApiCompanionAd(
                Urn.forAd("dfp", "746"),
                "http://image.visualad.com",
                clickthrough,
                Arrays.asList("comp_impression1", "comp_impression2"),
                Arrays.asList("comp_click1", "comp_click2"),
                "LEARN MORE",
                getApiDisplayProperties()
        );
    }

    private static ApiCompanionAd getApiCompanionAdWithCustomCTA(String ctaText) {
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

    private static ApiLeaveBehind getApiLeaveBehind() {
        return new ApiLeaveBehind(
                Urn.forAd("dfp", "35"),
                "http://image.visualad.com",
                "http://clickthrough.visualad.com",
                Arrays.asList("leave_impression1", "leave_impression2"),
                Arrays.asList("leave_click1", "leave_click2")
        );
    }

    static ApiAudioAd getApiAudioAd() {
        return getApiAudioAdWithCompanion(getApiCompanionAd(), SKIPPABLE);
    }

    private static ApiAudioAd getApiAudioAd(boolean skippable) {
        return getApiAudioAdWithCompanion(getApiCompanionAd(), skippable);
    }

    private static ApiAudioAd getNonClickableApiAudioAd() {
        final ApiCompanionAd nonClickableCompanion = new ApiCompanionAd(
                Urn.forAd("dfp", "companion"),
                "imageurl",
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                getApiDisplayProperties()
        );
        return getApiAudioAdWithCompanion(nonClickableCompanion, SKIPPABLE);
    }

    private static ApiAudioAd getApiAudioAdWithCompanion(ApiCompanionAd companion, boolean skippable) {
        RelatedResources relatedResources = new RelatedResources(companion, getApiLeaveBehind());
        return new ApiAudioAd(
                Urn.forAd("dfp", "869"),
                skippable,
                relatedResources,
                getApiAudioAdSources(),
                getApiAudioAdTracking()
        );
    }

    private static ApiAudioAd getCompanionlessAudioAd() {
        RelatedResources relatedResources = new RelatedResources(null, getApiLeaveBehind());
        return new ApiAudioAd(
                Urn.forAd("dfp", "869"),
                true,
                relatedResources,
                getApiAudioAdSources(),
                getApiAudioAdTracking()
        );
    }

    static ApiAudioAd getApiAudioAdWithoutLeaveBehind() {
        RelatedResources relatedResources = new RelatedResources(getApiCompanionAd(), null);
        return new ApiAudioAd(
                Urn.forAd("dfp", "869"),
                SKIPPABLE,
                relatedResources,
                getApiAudioAdSources(),
                getApiAudioAdTracking()
        );
    }

    private static List<ApiAudioAdSource> getApiAudioAdSources() {
        return Arrays.asList(ApiAudioAdSource.create("audio/mpeg", "http://audiourl.com/audio.mp3", false),
                             ApiAudioAdSource.create("application/x-mpegurl", "http://audiourl.com/audio.m3u", true));
    }

    public static ApiVideoSource getApiVideoSource(int width, int height) {
        return getApiVideoSource(width, height, "video/mp4", VIDEO_BIT_RATE);
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

    private static ApiAdTracking getApiAudioAdTracking() {
        return new ApiAdTracking(
                Collections.emptyList(),
                Arrays.asList("audio_impression1", "audio_impression2"),
                Arrays.asList("audio_skip1", "audio_skip2"),
                Arrays.asList("audio_start_1", "audio_start_2"),
                Arrays.asList("audio_quartile1_1", "audio_quartile1_2"),
                Arrays.asList("audio_quartile2_1", "audio_quartile2_2"),
                Arrays.asList("audio_quartile3_1", "audio_quartile3_2"),
                Arrays.asList("audio_finish1", "audio_finish2"),
                Arrays.asList("audio_pause1", "audio_pause2"),
                Arrays.asList("audio_resume1", "audio_resume2"),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private static ApiAdTracking getApiVideoAdTracking() {
        return new ApiAdTracking(
                Arrays.asList("video_click1", "video_click2"),
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

    static ApiVideoAd getApiVideoAd() {
        return getApiVideoAd(SKIPPABLE);
    }

    static ApiVideoAd getApiVideoAd(String title, String clickThroughText) {
        return getApiVideoAd(Collections.singletonList(getApiVideoSource(608, 1080)),
                             SKIPPABLE,
                             Optional.of(title),
                             Optional.of(clickThroughText));
    }

    private static ApiVideoAd getApiVideoAd(boolean skippable) {
        return getApiVideoAd(getApiVideoSource(608, 1080), skippable);
    }

    private static ApiVideoAd getApiVideoAd(ApiVideoSource apiVideoSource, boolean skippable) {
        return getApiVideoAd(Collections.singletonList(apiVideoSource), skippable, Optional.absent(), Optional.absent());
    }

    private static ApiVideoAd getApiVideoAd(List<ApiVideoSource> apiVideoSources,
                                            boolean skippable,
                                            Optional<String> title,
                                            Optional<String> clickthroughText) {
        return ApiVideoAd.create(
                Urn.forAd("dfp", "905"),
                60,
                title,
                clickthroughText,
                "http://clickthrough.videoad.com",
                getApiDisplayProperties(),
                apiVideoSources,
                getApiVideoAdTracking(),
                skippable
        );
    }

    static List<AdData> getInlays() {
        return getInlays(424242);
    }

    private static List<AdData> getInlays(long createdAt) {
        return Arrays.asList(getVideoAd(createdAt),
                             AppInstallAd.create(getApiAppInstall(Urn.forAd("dfp", "1")), createdAt),
                             AppInstallAd.create(getApiAppInstall(Urn.forAd("dfp", "2")), createdAt));
    }

    public static List<AppInstallAd> getAppInstalls() {
        return getAppInstalls(42424242);
    }

    private static List<AppInstallAd> getAppInstalls(long createdAt) {
        return Arrays.asList(AppInstallAd.create(getApiAppInstall(Urn.forAd("dfp", "1")), createdAt),
                             AppInstallAd.create(getApiAppInstall(Urn.forAd("dfp", "2")), createdAt));
    }

    private static ApiAdTracking getApiAppInstallTracking() {
        return new ApiAdTracking(
                Arrays.asList("app_click1", "app_click2"),
                Arrays.asList("app_impression1", "app_impression2"),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    public static ApiAppInstallAd getApiAppInstall() {
       return getApiAppInstall(Urn.forAd("dfp", "111"));
    }

    private static ApiAppInstallAd getApiAppInstall(Urn urn) {
        return ApiAppInstallAd.create(
                urn,
                60,
                "App Name",
                "Download",
                "http://clickthrough.com",
                "http://imageurl.com/image.png",
                3.1f,
                4411,
                getApiAppInstallTracking()
        );
    }

    static ApiAdsForTrack interstitialAdsForTrack() {
        return new ApiAdsForTrack(newArrayList(
                ApiAdWrapper.create(getApiInterstitial()))
        );
    }

    static ApiAdsForTrack audioAdsForTrack() {
        return new ApiAdsForTrack(newArrayList(
                ApiAdWrapper.create(getApiAudioAd()))
        );
    }

    static ApiAdsForTrack fullAdsForTrack() {
        return new ApiAdsForTrack(newArrayList(
                ApiAdWrapper.create(getApiAudioAd()),
                ApiAdWrapper.create(getApiVideoAd()),
                ApiAdWrapper.create(getApiInterstitial()))
        );
    }

    static ApiAdsForStream fullAdsForStream() {
        return new ApiAdsForStream(newArrayList(
                ApiAdWrapper.create(getApiAppInstall()),
                ApiAdWrapper.create(getApiVideoAd())
        ));
    }
}
