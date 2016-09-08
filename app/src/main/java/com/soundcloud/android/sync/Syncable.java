package com.soundcloud.android.sync;

import java.util.EnumSet;

public enum Syncable {
    /**
     * Used from Operations class
     * through {@link SyncInitiator}
     */
    SOUNDSTREAM,
    ACTIVITIES,
    TRACK_LIKES,
    PLAYLIST_LIKES,
    TRACK_POSTS,
    MY_PLAYLISTS,
    MY_FOLLOWINGS,
    ME,
    RECENT_STATIONS,
    LIKED_STATIONS,
    RECOMMENDED_STATIONS,
    RECOMMENDED_TRACKS,
    CHARTS,
    CHART_GENRES,
    /**
     * Used from Operations class
     * through {@link SyncInitiator}
     */
    TRACKS,
    PLAYLISTS,
    USERS,
    PLAYLIST,
    PLAY_HISTORY,
    RECENTLY_PLAYED;

    public static EnumSet<Syncable> FOREGROUND_ONLY = EnumSet.of(TRACKS, PLAYLISTS, USERS, PLAYLIST, PLAY_HISTORY);
}
