package com.soundcloud.android.properties;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.annotations.VisibleForTesting;

import android.support.annotation.NonNull;

import java.util.EnumSet;
import java.util.Locale;

public enum Flag {
    @VisibleForTesting
    TEST_FEATURE(State.DEFAULT_SHOW.name()),
    @VisibleForTesting
    TEST_FEATURE_UNDER_DEVELOPMENT(State.UNDER_DEVELOPMENT.name()),

    REMOTE_FEATURE_TOGGLES(BuildConfig.REMOTE_FEATURE_TOGGLES),
    FILTER_COLLECTIONS(BuildConfig.FILTER_COLLECTIONS),
    ARCHER_PUSH(BuildConfig.ARCHER_PUSH),
    MID_TIER(BuildConfig.MID_TIER),
    EDIT_PLAYLIST(BuildConfig.EDIT_PLAYLIST),
    HOLISTIC_TRACKING(BuildConfig.HOLISTIC_TRACKING),
    PLAY_QUEUE(BuildConfig.PLAY_QUEUE),
    SUGGESTED_CREATORS(BuildConfig.SUGGESTED_CREATORS),
    FORCE_SUGGESTED_CREATORS_FOR_ALL(BuildConfig.FORCE_SUGGESTED_CREATORS_FOR_ALL),
    NEW_HOME(BuildConfig.NEW_HOME),
    STREAM_HIGHLIGHTS(BuildConfig.STREAM_HIGHLIGHTS),
    OTHER_PLAYLISTS_BY_CREATOR(BuildConfig.OTHER_PLAYLISTS_BY_CREATOR),
    PROFILE_BANNER(BuildConfig.PROFILE_BANNER),
    CLEAR_TABLES_ON_SIGNOUT(BuildConfig.CLEAR_TABLES_ON_SIGNOUT),
    AUTOCOMPLETE(BuildConfig.AUTOCOMPLETE),
    CAST_V3(BuildConfig.CAST_V3),
    ADJUST_DEFERRED_DEEPLINKS(BuildConfig.ADJUST_DEFERRED_DEEPLINKS),
    FLIPPER(BuildConfig.FLIPPER),
    APPBOY(BuildConfig.APPBOY),
    WELCOME_USER(BuildConfig.WELCOME_USER),
    FORCE_SHOW_WELCOME_USER(BuildConfig.FORCE_SHOW_WELCOME_USER),
    ALIGNED_USER_INFO(BuildConfig.ALIGNED_USER_INFO),
    OFFLINE_PROPERTIES_PROVIDER(BuildConfig.OFFLINE_PROPERTIES_PROVIDER),
    VIDEO_INLAYS(BuildConfig.VIDEO_INLAYS);

    private final State state;

    Flag(String state) {
        this.state = State.valueOf(state);
    }

    @NonNull
    public String featureName() {
        return this.name().toLowerCase(Locale.US);
    }

    public boolean featureValue() {
        return !(isUnderDevelopment() || state == State.DEFAULT_HIDE);
    }

    public boolean isUnderDevelopment() {
        return state == State.UNDER_DEVELOPMENT;
    }

    public static EnumSet<Flag> features() {
        return EnumSet.complementOf(EnumSet.of(TEST_FEATURE, TEST_FEATURE_UNDER_DEVELOPMENT));
    }

    private enum State {
        DEFAULT_SHOW,
        DEFAULT_HIDE,
        UNDER_DEVELOPMENT
    }
}

