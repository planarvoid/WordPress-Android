package com.soundcloud.android.ads;

import com.soundcloud.android.model.Urn;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// Temporary video ad generator for demoing purposes on debug & alpha builds.
// TODO: Remove when video ads are handled by api-mobile and the promoted service
class DemoVideoAdProvider {

    private static final String MP4_TYPE = "video/mp4";
    private static final String VERTICAL_SOURCE_LOW = "https://va.sndcdn.com/vertical-618.mp4";
    private static final String VERTICAL_SOURCE_HIGH = "https://va.sndcdn.com/vertical-919.mp4";
    private static final String LETTERBOX_SOURCE_LOW = "https://va.sndcdn.com/letterbox-549.mp4";
    private static final String LETTERBOX_SOURCE_HIGH = "https://va.sndcdn.com/letterbox-1970.mp4";

    private static final Random PRNG = new Random();

    private DemoVideoAdProvider() {}

    public static ApiVideoAd getRandomVideo() {
        return createVideo(PRNG.nextBoolean() ? getVerticalDemoSources() : getLetterboxDemoSources());
    }

    private static ApiVideoAd createVideo(List<ApiVideoSource> videoAdSources) {
        return ApiVideoAd.create(
                Urn.forAd("demo-video", Integer.toString(PRNG.nextInt())),
                videoAdSources,
                emptyTrackingUrls(),
                createCompanionAd());
    }

    private static List<ApiVideoSource> getVerticalDemoSources() {
        return Arrays.asList(
                ApiVideoSource.create(MP4_TYPE, VERTICAL_SOURCE_LOW, 618, 406, 720),
                ApiVideoSource.create(MP4_TYPE, VERTICAL_SOURCE_HIGH, 919, 608, 1080));
    }

    private static List<ApiVideoSource> getLetterboxDemoSources() {
        return Arrays.asList(
                ApiVideoSource.create(MP4_TYPE, LETTERBOX_SOURCE_LOW, 549, 640, 360),
                ApiVideoSource.create(MP4_TYPE, LETTERBOX_SOURCE_HIGH, 1970, 1280, 720));
    }

    private static ApiVideoTracking emptyTrackingUrls() {
        return new ApiVideoTracking(
                Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(),
                Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(),
                Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(),
                Collections.<String>emptyList(), Collections.<String>emptyList());
    }

    private static ApiCompanionAd createCompanionAd() {
        return new ApiCompanionAd(
                Urn.forAd("demo-companion", Integer.toString(PRNG.nextInt())),
                "http://unused-image-link",
                "http://soundcloud.com/explore",
                Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                null,
                createDefaultDisplayProperties());
    }

    private static ApiDisplayProperties createDefaultDisplayProperties() {
        return new ApiDisplayProperties(
                "#FFFFFF", "#000000",
                "#FFFFFF", "#333333",
                "#FFFFFF", "#333333");
    }

}
