package com.soundcloud.android.sync;

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
    PLAYLIST_POSTS, //Is this necessary? We sync Post with MY_PLAYLISTS
    MY_PLAYLISTS,
    RECENT_STATIONS,
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
    PLAYLIST
}
