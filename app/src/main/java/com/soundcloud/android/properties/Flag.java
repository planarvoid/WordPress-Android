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
    FEATURE_NEW_SYNC_ADAPTER(BuildConfig.NEW_SYNC_ADAPTER),
    ARCHER_PUSH(BuildConfig.ARCHER_PUSH),
    MID_TIER(BuildConfig.MID_TIER),
    DISCOVERY_RECOMMENDATIONS(BuildConfig.DISCOVERY_RECOMMENDATIONS),
    DISCOVERY_CHARTS(BuildConfig.DISCOVERY_CHARTS),
    EDIT_PLAYLIST(BuildConfig.EDIT_PLAYLIST),
    HOLISTIC_TRACKING(BuildConfig.HOLISTIC_TRACKING),
    EXPLORE(BuildConfig.EXPLORE),
    PLAY_QUEUE(BuildConfig.PLAY_QUEUE),
    STATION_INFO_PAGE(BuildConfig.STATION_PAGE),
    LIKED_STATIONS(BuildConfig.LIKED_STATIONS),
    SUGGESTED_CREATORS(BuildConfig.SUGGESTED_CREATORS),
    FORCE_SUGGESTED_CREATORS_FOR_ALL(BuildConfig.FORCE_SUGGESTED_CREATORS_FOR_ALL),
    MOAT_ADS_VIEWABILITY(BuildConfig.MOAT_ADS_VIEWABILITY);

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

