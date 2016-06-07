package com.soundcloud.android.collection;

import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.tracks.TrackItem;

import java.util.List;

public class MyCollection {

    private final LikesItem likes;
    private final List<PlaylistItem> likedAndPostedPlaylists;
    private final List<StationRecord> recentStations;
    private final List<TrackItem> playHistoryTrackItems;
    private final boolean atLeastOneError;

    MyCollection(LikesItem likes, List<PlaylistItem> likedAndPostedPlaylists,
                        List<StationRecord> recentStations,
                        List<TrackItem> playHistoryTrackItems,
                        boolean atLeastOneError) {
        this.likes = likes;
        this.likedAndPostedPlaylists = likedAndPostedPlaylists;
        this.recentStations = recentStations;
        this.playHistoryTrackItems = playHistoryTrackItems;
        this.atLeastOneError = atLeastOneError;
    }

    public List<PlaylistItem> getPlaylistItems() {
        return likedAndPostedPlaylists;
    }

    public LikesItem getLikes() {
        return likes;
    }

    List<StationRecord> getRecentStations() {
        return recentStations;
    }

    List<TrackItem> getPlayHistoryTrackItems() {
        return playHistoryTrackItems;
    }

    boolean hasError() {
        return atLeastOneError;
    }

}
