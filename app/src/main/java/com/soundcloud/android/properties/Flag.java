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
    MID_TIER_ROLLOUT(BuildConfig.MID_TIER_ROLLOUT),
    NEW_PLAYLIST_SCREEN(BuildConfig.NEW_PLAYLIST_SCREEN),
    HOLISTIC_TRACKING(BuildConfig.HOLISTIC_TRACKING),
    SUGGESTED_CREATORS(BuildConfig.SUGGESTED_CREATORS),
    FORCE_SUGGESTED_CREATORS_FOR_ALL(BuildConfig.FORCE_SUGGESTED_CREATORS_FOR_ALL),
    NEW_HOME(BuildConfig.NEW_HOME),
    NEW_FOR_YOU_FIRST(BuildConfig.NEW_FOR_YOU_FIRST),
    NEW_FOR_YOU_SECOND(BuildConfig.NEW_FOR_YOU_SECOND),
    STREAM_HIGHLIGHTS(BuildConfig.STREAM_HIGHLIGHTS),
    OTHER_PLAYLISTS_BY_CREATOR(BuildConfig.OTHER_PLAYLISTS_BY_CREATOR),
    PROFILE_BANNER(BuildConfig.PROFILE_BANNER),
    CLEAR_TABLES_ON_SIGNOUT(BuildConfig.CLEAR_TABLES_ON_SIGNOUT),
    AUTOCOMPLETE(BuildConfig.AUTOCOMPLETE),
    SEARCH_TOP_RESULTS(BuildConfig.SEARCH_TOP_RESULTS),
    CAST_V3(BuildConfig.CAST_V3),
    ADJUST_DEFERRED_DEEPLINKS(BuildConfig.ADJUST_DEFERRED_DEEPLINKS),
    FLIPPER(BuildConfig.FLIPPER),
    FLIPPER_PRELOAD(BuildConfig.FLIPPER_PRELOAD),
    WELCOME_USER(BuildConfig.WELCOME_USER),
    FORCE_SHOW_WELCOME_USER(BuildConfig.FORCE_SHOW_WELCOME_USER),
    ALIGNED_USER_INFO(BuildConfig.ALIGNED_USER_INFO),
    OFFLINE_PROPERTIES_PROVIDER(BuildConfig.OFFLINE_PROPERTIES_PROVIDER),
    VIDEO_INLAYS(BuildConfig.VIDEO_INLAYS),
    SEARCH_PLAY_RELATED_TRACKS(BuildConfig.SEARCH_PLAY_RELATED_TRACKS),
    AUTH_API_MOBILE(BuildConfig.AUTH_API_MOBILE),
    RECOMMENDED_PLAYLISTS(BuildConfig.RECOMMENDED_PLAYLISTS),
    DYNAMIC_LINK_SHARING(BuildConfig.DYNAMIC_LINK_SHARING);

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

