package com.soundcloud.android.collection;

import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.stations.StationRecord;

import java.util.List;

public class MyCollection {

    private final LikesItem likes;
    private final List<PlaylistItem> likedAndPostedPlaylists;
    private final List<StationRecord> recentStations;
    private final boolean atLeastOneError;

    public MyCollection(LikesItem likes, List<PlaylistItem> likedAndPostedPlaylists,
                        List<StationRecord> recentStations, boolean atLeastOneError) {
        this.likes = likes;
        this.likedAndPostedPlaylists = likedAndPostedPlaylists;
        this.recentStations = recentStations;
        this.atLeastOneError = atLeastOneError;
    }

    public List<PlaylistItem> getPlaylistItems() {
        return likedAndPostedPlaylists;
    }

    public LikesItem getLikes() {
        return likes;
    }

    public List<StationRecord> getRecentStations() {
        return recentStations;
    }

    public boolean hasError() {
        return atLeastOneError;
    }
}
