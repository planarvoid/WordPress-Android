package com.soundcloud.android.properties;

import com.soundcloud.android.BuildConfig;

import java.util.EnumSet;
import java.util.Locale;

public enum Flag {

    TEST_FEATURE(true),
    STATIONS_HOME(BuildConfig.STATIONS_HOME),
    FEATURE_PUBLISH_PLAY_EVENTS_TO_TPUB(BuildConfig.PUBLISH_PLAY_EVENTS_TO_TPUB),
    DISCOVERY_RECOMMENDATIONS(BuildConfig.DISCOVERY_RECOMMENDATIONS),
    DISCOVERY_CHARTS(BuildConfig.DISCOVERY_CHARTS),
    FEATURE_PROFILE_NEW_TABS(BuildConfig.PROFILE_NEW_TABS),
    EXPLODE_PLAYLISTS_IN_PLAYQUEUES(BuildConfig.EXPLODE_PLAYLISTS_IN_PLAYQUEUES),
    NEW_SEARCH_SUGGESTIONS(BuildConfig.NEW_SEARCH_SUGGESTIONS),
    EDIT_PLAYLIST(BuildConfig.EDIT_PLAYLIST),
    USER_STATIONS(BuildConfig.USER_STATIONS),
    RECOMMENDED_STATIONS(BuildConfig.RECOMMENDED_STATIONS);

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
