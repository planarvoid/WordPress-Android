package com.soundcloud.android.properties;

import com.soundcloud.android.BuildConfig;

import java.util.EnumSet;
import java.util.Locale;

public enum Flag {

    TEST_FEATURE(true),
    PAYMENTS_TEST(BuildConfig.FEATURE_PAYMENTS_TEST),
    SOUNDCLOUD_GO(BuildConfig.FEATURE_SOUNDCLOUD_GO),
    STATIONS_HOME(BuildConfig.FEATURE_STATIONS_HOME),
    FEATURE_PUBLISH_PLAY_EVENTS_TO_TPUB(BuildConfig.FEATURE_PUBLISH_PLAY_EVENTS_TO_TPUB),
    DISCOVERY_RECOMMENDATIONS(BuildConfig.FEATURE_DISCOVERY_RECOMMENDATIONS),
    DISCOVERY_CHARTS(BuildConfig.FEATURE_DISCOVERY_CHARTS),
    FEATURE_PROFILE_NEW_TABS(BuildConfig.FEATURE_PROFILE_NEW_TABS),
    EXPLODE_PLAYLISTS_IN_PLAYQUEUES(BuildConfig.FEATURE_EXPLODE_PLAYLISTS_IN_PLAYQUEUES),
    FEATURE_WEB_UPGRADE_FLOW(BuildConfig.FEATURE_WEB_UPGRADE_FLOW),
    VIDEO_ADS(BuildConfig.FEATURE_VIDEO_ADS),
    SERVE_DEMO_VIDEO_AD(BuildConfig.FEATURE_SERVE_DEMO_VIDEO_AD),
    WAVEFORM_SPRING(BuildConfig.FEATURE_WAVEFORM_SPRING),
    NEW_NOTIFICATION_SETTINGS(BuildConfig.FEATURE_NEW_NOTIFICATION_SETTINGS),
    AUTO_REFRESH_STREAM(BuildConfig.FEATURE_AUTO_REFRESH_STREAM);

    private final boolean value;

    Flag(boolean value) {
        this.value = value;
    }

    public String getName() {
        return this.name().toLowerCase(Locale.US);
    }

    public boolean getValue() {
        return value;
    }

    public static EnumSet<Flag> realFeatures() {
        return EnumSet.complementOf(EnumSet.of(TEST_FEATURE, PAYMENTS_TEST));
    }
}
