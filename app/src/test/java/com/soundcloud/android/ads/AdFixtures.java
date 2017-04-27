package com.soundcloud.android.ads;

import static com.soundcloud.android.model.Urn.forAd;
import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.ads.AudioAd.ApiModel.RelatedResources;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AdFixtures {

    private static final boolean NOT_SKIPPABLE = false;
    private static final int VIDEO_BIT_RATE = 1001;
    private static final boolean SKIPPABLE = true;


    public static VisualPrestitialAd visualPrestitialAd() {
        return VisualPrestitialAd.create(getApiVisualPrestitial());
    }

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
                getApiAudioAdWithCompanion(getApiCompanionAdWithCustomCTA(Optional.of(ctaText)), SKIPPABLE),
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

    public static VideoAd getInlayVideoAd(long createdAt) {
        return VideoAd.create(getApiVideoAd(), createdAt, AdData.MonetizationType.INLAY);
    }

    public static VideoAd getVideoAd(Urn adUrn, Urn monetizableUrn) {
        return VideoAd.createWithMonetizableTrack(getApiVideoAd(adUrn), 0, monetizableUrn);
    }

    public static VideoAd getVideoAd(Urn monetizableUrn) {
        return VideoAd.createWithMonetizableTrack(getApiVideoAd(), 0, monetizableUrn);
    }

    public static VideoAd getVideoAd(Urn monetizableUrn, VideoAdSource.ApiModel videoSource) {
        return VideoAd.createWithMonetizableTrack(getApiVideoAd(videoSource, SKIPPABLE), 0, monetizableUrn);
    }

    public static VideoAd getVideoAd(Urn monetizableUrn, List<VideoAdSource.ApiModel> videoSources) {
        final Urn adUrn = Urn.forAd("dfp", "905");
        return VideoAd.createWithMonetizableTrack(getApiVideoAd(adUrn, videoSources, SKIPPABLE, Optional.absent(), Optional.absent()), 0, monetizableUrn);
    }

    public static VideoAd getNonskippableVideoAd(Urn monetizableUrn) {
        return VideoAd.createWithMonetizableTrack(getApiVideoAd(NOT_SKIPPABLE), 0, monetizableUrn);
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

    private static AudioAd.ApiModel.Companion getApiCompanionAd() {
        return getApiCompanionAdWithCustomCTA(Optional.absent());
    }

    private static AudioAd.ApiModel.Companion getApiCompanionAdWithCustomClickthrough(String clickthrough) {
        return AudioAd.ApiModel.Companion.create(forAd("dfp", "746"),
                                                 "http://image.visualad.com",
                                                 clickthrough,
                                                 Arrays.asList("comp_impression1", "comp_impression2"),
                                                 Arrays.asList("comp_click1", "comp_click2"),
                                                 Optional.of("LEARN MORE"),
                                                 getApiDisplayProperties());
    }

    private static AudioAd.ApiModel.Companion getApiCompanionAdWithCustomCTA(Optional<String> ctaText) {
        return AudioAd.ApiModel.Companion.create(forAd("dfp", "746"),
                                                 "http://image.visualad.com",
                                                 "http://clickthrough.visualad.com",
                                                 Arrays.asList("comp_impression1", "comp_impression2"),
                                                 Arrays.asList("comp_click1", "comp_click2"),
                                                 ctaText,
                                                 getApiDisplayProperties());
    }

    static InterstitialAd.ApiModel getApiInterstitial() {
        return InterstitialAd.ApiModel.create(forAd("dfp", "35"),
                                              "http://image.visualad.com",
                                              "http://clickthrough.visualad.com",
                                              Arrays.asList("interstitial_impression1", "intersitial_impression2"),
                                              Arrays.asList("interstitial_click1", "interstitial_click2"));
    }

    private static LeaveBehindAd.ApiModel getApiLeaveBehind() {
        return LeaveBehindAd.ApiModel.create(forAd("dfp", "35"),
                                             "http://image.visualad.com",
                                             "http://clickthrough.visualad.com",
                                             Arrays.asList("leave_impression1", "leave_impression2"),
                                             Arrays.asList("leave_click1", "leave_click2"));
    }

    public static VisualPrestitialAd.ApiModel getApiVisualPrestitial() {
        return VisualPrestitialAd.ApiModel.create(Urn.forAd("ads", "123"),
                                                  "http://image-url.com",
                                                  "http://clickthrough.com",
                                                  Arrays.asList("visual_impression1", "visual_impression2"),
                                                  Arrays.asList("visual_click1", "visual_click2"));
    }

    static AudioAd.ApiModel getApiAudioAd() {
        return getApiAudioAdWithCompanion(getApiCompanionAd(), SKIPPABLE);
    }

    private static AudioAd.ApiModel getApiAudioAd(boolean skippable) {
        return getApiAudioAdWithCompanion(getApiCompanionAd(), skippable);
    }

    private static AudioAd.ApiModel getNonClickableApiAudioAd() {
        final AudioAd.ApiModel.Companion nonClickableCompanion = AudioAd.ApiModel.Companion.create(forAd("dfp", "companion"),
                                                                                                   "imageurl",
                                                                                                   "",
                                                                                                   Collections.emptyList(),
                                                                                                   Collections.emptyList(),
                                                                                                   Optional.absent(),
                                                                                                   getApiDisplayProperties());
        return getApiAudioAdWithCompanion(nonClickableCompanion, SKIPPABLE);
    }

    private static AudioAd.ApiModel getApiAudioAdWithCompanion(AudioAd.ApiModel.Companion companion, boolean skippable) {
        RelatedResources relatedResources = RelatedResources.create(Optional.of(companion), Optional.of(getApiLeaveBehind()));
        return AudioAd.ApiModel.create(forAd("dfp", "869"),
                                       skippable,
                                       relatedResources,
                                       getApiAudioAdSources(),
                                       getApiAudioAdTracking());
    }

    private static AudioAd.ApiModel getCompanionlessAudioAd() {
        RelatedResources relatedResources = RelatedResources.create(Optional.absent(), Optional.of(getApiLeaveBehind()));
        return AudioAd.ApiModel.create(forAd("dfp", "869"),
                                       true,
                                       relatedResources,
                                       getApiAudioAdSources(),
                                       getApiAudioAdTracking());
    }

    static AudioAd.ApiModel getApiAudioAdWithoutLeaveBehind() {
        RelatedResources relatedResources = RelatedResources.create(Optional.of(getApiCompanionAd()), Optional.absent());
        return AudioAd.ApiModel.create(forAd("dfp", "869"),
                                       SKIPPABLE,
                                       relatedResources,
                                       getApiAudioAdSources(),
                                       getApiAudioAdTracking());
    }

    private static List<AudioAdSource.ApiModel> getApiAudioAdSources() {
        return Arrays.asList(AudioAdSource.ApiModel.create("audio/mpeg", "http://audiourl.com/audio.mp3", false),
                             AudioAdSource.ApiModel.create("application/x-mpegurl", "http://audiourl.com/audio.m3u", true));
    }

    public static VideoAdSource.ApiModel getApiVideoSource(int width, int height) {
        return getApiVideoSource(width, height, "video/mp4", VIDEO_BIT_RATE);
    }

    public static VideoAdSource.ApiModel getApiVideoSource(int width, int height, String type, int bitRate) {
        return VideoAdSource.ApiModel.create(type,
                                             "http://videourl.com/video.mp4",
                                             bitRate,
                                             width,
                                             height);
    }

    private static ApiAdTracking getApiAudioAdTracking() {
        return ApiAdTracking.create(
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
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private static ApiAdTracking getApiVideoAdTracking() {
        return ApiAdTracking.create(Arrays.asList("video_click1", "video_click2"),
                                    Arrays.asList("video_impression1", "video_impression2"),
                                    Arrays.asList("video_skip1", "video_skip2"),
                                    Arrays.asList("video_start1", "video_start2"),
                                    Arrays.asList("video_quartile1_1", "video_quartile1_2"),
                                    Arrays.asList("video_quartile2_1", "video_quartile2_2"),
                                    Arrays.asList("video_quartile3_1", "video_quartile3_2"),
                                    Arrays.asList("video_finish1", "video_finish2"),
                                    Arrays.asList("video_pause1", "video_pause2"),
                                    Arrays.asList("video_resume1", "video_resume2"),
                                    Arrays.asList("video_mute1", "video_mute2"),
                                    Arrays.asList("video_unmute1", "video_unmute2"),
                                    Arrays.asList("video_fullscreen1", "video_fullscreen2"),
                                    Arrays.asList("video_exit_full1", "video_exit_full2"));
    }

    static VideoAd.ApiModel getApiVideoAd() {
        return getApiVideoAd(SKIPPABLE);
    }

    static VideoAd.ApiModel getApiVideoAd(String title, String clickThroughText) {
        final Urn adUrn = Urn.forAd("dfp", "905");
        return getApiVideoAd(adUrn,
                             Collections.singletonList(getApiVideoSource(608, 1080)),
                             SKIPPABLE,
                             Optional.of(title),
                             Optional.of(clickThroughText));
    }

    private static VideoAd.ApiModel getApiVideoAd(Urn adUrn) {
        return getApiVideoAd(adUrn, Collections.singletonList(getApiVideoSource(608, 1080)), NOT_SKIPPABLE, Optional.absent(), Optional.absent());
    }

    private static VideoAd.ApiModel getApiVideoAd(boolean skippable) {
        return getApiVideoAd(getApiVideoSource(608, 1080), skippable);
    }

    private static VideoAd.ApiModel getApiVideoAd(VideoAdSource.ApiModel apiVideoSource, boolean skippable) {
        final Urn adUrn = Urn.forAd("dfp", "905");
        return getApiVideoAd(adUrn, Collections.singletonList(apiVideoSource), skippable, Optional.absent(), Optional.absent());
    }

    private static VideoAd.ApiModel getApiVideoAd(Urn adUrn,
                                                  List<VideoAdSource.ApiModel> apiVideoSources,
                                                  boolean skippable,
                                                  Optional<String> title,
                                                  Optional<String> clickthroughText) {
        return VideoAd.ApiModel.create(adUrn,
                                       60,
                                       30L,
                                       title,
                                       clickthroughText,
                                       "http://clickthrough.videoad.com",
                                       getApiDisplayProperties(),
                                       apiVideoSources,
                                       getApiVideoAdTracking(),
                                       skippable);
    }

    static List<AdData> getInlays() {
        return getInlays(424242);
    }

    private static List<AdData> getInlays(long createdAt) {
        return Arrays.asList(getInlayVideoAd(createdAt),
                             AppInstallAd.create(getApiAppInstall(forAd("dfp", "1")), createdAt),
                             AppInstallAd.create(getApiAppInstall(forAd("dfp", "2")), createdAt));
    }

    public static List<AppInstallAd> getAppInstalls() {
        return getAppInstalls(42424242);
    }

    private static List<AppInstallAd> getAppInstalls(long createdAt) {
        return Arrays.asList(AppInstallAd.create(getApiAppInstall(forAd("dfp", "1")), createdAt),
                             AppInstallAd.create(getApiAppInstall(forAd("dfp", "2")), createdAt));
    }

    private static ApiAdTracking getApiAppInstallTracking() {
        return ApiAdTracking.create(Arrays.asList("app_click1", "app_click2"),
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
                                    Collections.emptyList(),
                                    Collections.emptyList(),
                                    Collections.emptyList());
    }

    public static AppInstallAd.ApiModel getApiAppInstall() {
       return getApiAppInstall(forAd("dfp", "111"));
    }

    private static AppInstallAd.ApiModel getApiAppInstall(Urn urn) {
        return AppInstallAd.ApiModel.create(urn, 60, "App Name", "Download",
                                            "http://clickthrough.com",
                                            "http://imageurl.com/image.png",
                                            3.1f, 4411, getApiAppInstallTracking());
    }

    static ApiAdsForTrack interstitialAdsForTrack() {
        return new ApiAdsForTrack(newArrayList(ApiAdWrapper.create(getApiInterstitial())));
    }

    static ApiAdsForTrack audioAdsForTrack() {
        return new ApiAdsForTrack(newArrayList(ApiAdWrapper.create(getApiAudioAd())));
    }

    static ApiAdsForTrack fullAdsForTrack() {
        return new ApiAdsForTrack(newArrayList(ApiAdWrapper.create(getApiAudioAd()),
                                               ApiAdWrapper.create(getApiVideoAd()),
                                               ApiAdWrapper.create(getApiInterstitial())));
    }

    static ApiAdsForStream fullAdsForStream() {
        return new ApiAdsForStream(newArrayList(ApiAdWrapper.create(getApiAppInstall()),
                                                ApiAdWrapper.create(getApiVideoAd())));
    }

    static ApiPrestitialAd apiPrestitialAd() {
        return new ApiPrestitialAd(Collections.singletonList(ApiAdWrapper.create(getApiVisualPrestitial())));
    }
}
