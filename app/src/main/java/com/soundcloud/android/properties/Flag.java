package com.soundcloud.android.properties;

import com.soundcloud.android.BuildConfig;

import java.util.EnumSet;
import java.util.Locale;

public enum Flag {

    TEST_FEATURE(true),
    PAYMENTS_TEST(BuildConfig.FEATURE_PAYMENTS_TEST),
    OFFLINE_SYNC(BuildConfig.FEATURE_OFFLINE_SYNC),
    FOLLOW_USER_SEARCH(BuildConfig.FEATURE_FOLLOW_USER_SEARCH),
    PLAY_RELATED_TRACKS(BuildConfig.FEATURE_PLAY_RELATED_TRACKS && !BuildConfig.FEATURE_STATIONS_SOFT_LAUNCH),
    // When removing this feature flag, make sure the ci does not exclude tests annotated with @StationsTest
    STATIONS_SOFT_LAUNCH(BuildConfig.FEATURE_STATIONS_SOFT_LAUNCH),
    STATIONS_HOME(BuildConfig.FEATURE_STATIONS_HOME && BuildConfig.FEATURE_STATIONS_SOFT_LAUNCH),
    EVENTLOGGER_AUDIO_V1(BuildConfig.FEATURE_EVENTLOGGER_AUDIO_V1),
    KILL_CONCURRENT_STREAMING(BuildConfig.FEATURE_KILL_CONCURRENT_STREAMING),
    FEATURE_PUBLISH_PLAY_EVENTS_TO_TPUB(BuildConfig.FEATURE_PUBLISH_PLAY_EVENTS_TO_TPUB),
    COLLECTIONS(BuildConfig.FEATURE_COLLECTIONS),
    APPBOY(BuildConfig.FEATURE_APPBOY),
    DAILY_POLICY_UPDATES(BuildConfig.FEATURE_DAILY_POLICY_UPDATES),
    DISCOVERY(BuildConfig.FEATURE_DISCOVERY),
    DISCOVERY_RECOMMENDATIONS(BuildConfig.FEATURE_DISCOVERY_RECOMMENDATIONS),
    DISCOVERY_CHARTS(BuildConfig.FEATURE_DISCOVERY_CHARTS),
    PROFILE_API_MOBILE(BuildConfig.FEATURE_PROFILE_API_MOBILE),
    NEW_STREAM(BuildConfig.FEATURE_NEW_STREAM),
    ACTIVITIES_REFACTOR(BuildConfig.FEATURE_ACTIVITIES_REFACTOR);

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
