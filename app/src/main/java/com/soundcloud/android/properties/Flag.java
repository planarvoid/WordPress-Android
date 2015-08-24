package com.soundcloud.android.properties;

import com.soundcloud.android.BuildConfig;

import java.util.EnumSet;

public enum Flag {

    TEST_FEATURE(true),
    PAYMENTS_TEST(BuildConfig.FEATURE_PAYMENTS_TEST),
    GOOGLE_CAST(BuildConfig.FEATURE_GOOGLE_CAST),
    OFFLINE_SYNC(BuildConfig.FEATURE_OFFLINE_SYNC),
    NEW_PROFILE(BuildConfig.FEATURE_NEW_PROFILE),
    FOLLOW_USER_SEARCH(BuildConfig.FEATURE_FOLLOW_USER_SEARCH),
    NEW_PROFILE_FRAGMENTS(BuildConfig.FEATURE_NEW_PROFILE_FRAGMENTS),
    PLAY_RELATED_TRACKS(BuildConfig.FEATURE_PLAY_RELATED_TRACKS),
    NEVER_ENDING_PLAY_QUEUE(BuildConfig.FEATURE_NEVER_ENDING_PLAY_QUEUE),
    STATIONS(BuildConfig.FEATURE_STATIONS),
    SEARCH_AND_RECOMMENDATIONS(BuildConfig.FEATURE_RECOMMENDATIONS),
    EVENTLOGGER_AUDIO_V1(BuildConfig.FEATURE_EVENTLOGGER_AUDIO_V1),
    RECOMMENDED_PLAYER_CONTEXT(BuildConfig.FEATURE_RECOMMENDED_PLAYER_CONTEXT);

    private final boolean value;

    Flag(boolean value) {
        this.value = value;
    }

    public String getName() {
        return this.name().toLowerCase();
    }

    public boolean getValue() {
        return value;
    }

    public static EnumSet<Flag> realFeatures() {
        return EnumSet.complementOf(EnumSet.of(TEST_FEATURE, PAYMENTS_TEST));
    }
}
