package com.soundcloud.android.properties;

import com.soundcloud.android.BuildConfig;

import java.util.EnumSet;
import java.util.Locale;

public enum Flag {

    TEST_FEATURE(true),
    SOUNDCLOUD_GO(BuildConfig.SOUNDCLOUD_GO),
    STATIONS_HOME(BuildConfig.STATIONS_HOME),
    FEATURE_PUBLISH_PLAY_EVENTS_TO_TPUB(BuildConfig.PUBLISH_PLAY_EVENTS_TO_TPUB),
    DISCOVERY_RECOMMENDATIONS(BuildConfig.DISCOVERY_RECOMMENDATIONS),
    DISCOVERY_CHARTS(BuildConfig.DISCOVERY_CHARTS),
    FEATURE_PROFILE_NEW_TABS(BuildConfig.PROFILE_NEW_TABS),
    EXPLODE_PLAYLISTS_IN_PLAYQUEUES(BuildConfig.EXPLODE_PLAYLISTS_IN_PLAYQUEUES),
    FEATURE_WEB_UPGRADE_FLOW(BuildConfig.WEB_UPGRADE_FLOW),
    VIDEO_ADS(BuildConfig.VIDEO_ADS),
    SERVE_DEMO_VIDEO_AD(BuildConfig.SERVE_DEMO_VIDEO_AD),
    WAVEFORM_SPRING(BuildConfig.WAVEFORM_SPRING),
    NEW_NOTIFICATION_SETTINGS(BuildConfig.NEW_NOTIFICATION_SETTINGS),
    AUTO_REFRESH_STREAM(BuildConfig.AUTO_REFRESH_STREAM);

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
        return EnumSet.complementOf(EnumSet.of(TEST_FEATURE));
    }
}
