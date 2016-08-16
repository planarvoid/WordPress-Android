package com.soundcloud.android.properties;

import com.soundcloud.android.BuildConfig;

import java.util.EnumSet;
import java.util.Locale;

public enum Flag {

    TEST_FEATURE(true),
    FEATURE_PUBLISH_PLAY_EVENTS_TO_TPUB(BuildConfig.PUBLISH_PLAY_EVENTS_TO_TPUB),
    FEATURE_NEW_SYNC_ADAPTER(BuildConfig.NEW_SYNC_ADAPTER),
    ARCHER_PUSH(BuildConfig.ARCHER_PUSH),
    DISCOVERY_RECOMMENDATIONS(BuildConfig.DISCOVERY_RECOMMENDATIONS),
    DISCOVERY_CHARTS(BuildConfig.DISCOVERY_CHARTS),
    EXPLODE_PLAYLISTS_IN_PLAYQUEUES(BuildConfig.EXPLODE_PLAYLISTS_IN_PLAYQUEUES),
    EDIT_PLAYLIST(BuildConfig.EDIT_PLAYLIST),
    LOCAL_PLAY_HISTORY(BuildConfig.LOCAL_PLAY_HISTORY),
    LOCAL_PLAY_HISTORY_STORAGE(BuildConfig.LOCAL_PLAY_HISTORY_STORAGE),
    ALBUMS(BuildConfig.ALBUMS),
    HOLISTIC_TRACKING(BuildConfig.HOLISTIC_TRACKING),
    PLAY_QUEUE(BuildConfig.PLAY_QUEUE),
    STATION_INFO_PAGE(BuildConfig.STATION_PAGE);

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
