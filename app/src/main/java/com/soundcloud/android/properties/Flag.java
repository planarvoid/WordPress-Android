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

    HOLISTIC_TRACKING(BuildConfig.HOLISTIC_TRACKING),
    SUGGESTED_CREATORS(BuildConfig.SUGGESTED_CREATORS),
    FORCE_SUGGESTED_CREATORS_FOR_ALL(BuildConfig.FORCE_SUGGESTED_CREATORS_FOR_ALL),
    NEW_HOME(BuildConfig.NEW_HOME),
    PROFILE_BANNER(BuildConfig.PROFILE_BANNER),
    SEARCH_TOP_RESULTS(BuildConfig.SEARCH_TOP_RESULTS),
    FLIPPER_V2(BuildConfig.FLIPPER_V2),
    ENCRYPTED_HLS(BuildConfig.ENCRYPTED_HLS),
    SEARCH_PLAY_RELATED_TRACKS(BuildConfig.SEARCH_PLAY_RELATED_TRACKS),
    DYNAMIC_LINK_SHARING(BuildConfig.DYNAMIC_LINK_SHARING),
    FIREBASE_PERFORMANCE_MONITORING(BuildConfig.FIREBASE_PERFORMANCE_MONITORING),
    CREATOR_APP_LINK(BuildConfig.CREATOR_APP_LINK),
    DATABASE_CLEANUP_SERVICE(BuildConfig.DATABASE_CLEANUP_SERVICE),
    FETCH_FEATURE_FLAGS_ON_LOGIN(BuildConfig.FETCH_FEATURE_FLAGS_ON_LOGIN),
    COMMENTS_ON_API_MOBILE(BuildConfig.COMMENTS_ON_API_MOBILE),
    UNIFLOW_NEW_HOME(BuildConfig.UNIFLOW_NEW_HOME),
    BOTTOM_NAVIGATION(BuildConfig.BOTTOM_NAVIGATION),
    LOCAL_SEARCH_HISTORY(BuildConfig.LOCAL_SEARCH_HISTORY);

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

