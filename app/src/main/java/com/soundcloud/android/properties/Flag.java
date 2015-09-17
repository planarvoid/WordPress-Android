package com.soundcloud.android.properties;

import com.soundcloud.android.BuildConfig;

import java.util.EnumSet;
import java.util.Locale;

public enum Flag {

    TEST_FEATURE(true),
    PAYMENTS_TEST(BuildConfig.FEATURE_PAYMENTS_TEST),
    OFFLINE_SYNC(BuildConfig.FEATURE_OFFLINE_SYNC),
    NEW_PROFILE(BuildConfig.FEATURE_NEW_PROFILE),
    FOLLOW_USER_SEARCH(BuildConfig.FEATURE_FOLLOW_USER_SEARCH),
    PLAY_RELATED_TRACKS(BuildConfig.FEATURE_PLAY_RELATED_TRACKS),
    STATIONS(BuildConfig.FEATURE_STATIONS),
    SEARCH_AND_RECOMMENDATIONS(BuildConfig.FEATURE_RECOMMENDATIONS),
    EVENTLOGGER_AUDIO_V1(BuildConfig.FEATURE_EVENTLOGGER_AUDIO_V1),
    RECOMMENDED_PLAYER_CONTEXT(BuildConfig.FEATURE_RECOMMENDED_PLAYER_CONTEXT),
    KILL_CONCURRENT_STREAMING(BuildConfig.FEATURE_KILL_CONCURRENT_STREAMING),
    COLLECTIONS(BuildConfig.FEATURE_COLLECTIONS),
    DAILY_POLICY_UPDATES(BuildConfig.FEATURE_DAILY_POLICY_UPDATES),

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
