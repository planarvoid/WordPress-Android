package com.soundcloud.android.collection.recentlyplayed;

abstract class RecentlyPlayedItem {

    public enum Kind {
        RecentlyPlayedHeader,
        RecentlyPlayedPlaylist,
        RecentlyPlayedStation,
        RecentlyPlayedProfile
    }

    abstract Kind getKind();
}
