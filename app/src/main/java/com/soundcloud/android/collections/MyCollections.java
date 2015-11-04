package com.soundcloud.android.collections;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;

import java.util.List;

public class MyCollections {

    private final List<Urn> likes;
    private final List<PlaylistItem> likedAndPostedPlaylists;
    private final List<Urn> recentStations;
    private final boolean atLeastOneError;

    public MyCollections(List<Urn> likes, List<PlaylistItem> likedAndPostedPlaylists,
                         List<Urn> recentStations, boolean atLeastOneError) {
        this.likes = likes;
        this.likedAndPostedPlaylists = likedAndPostedPlaylists;
        this.recentStations = recentStations;
        this.atLeastOneError = atLeastOneError;
    }

    public List<PlaylistItem> getPlaylistItems() {
        return likedAndPostedPlaylists;
    }

    public List<Urn> getLikes() {
        return likes;
    }

    public List<Urn> getRecentStations() {
        return recentStations;
    }

    public boolean hasError() {
        return atLeastOneError;
    }
}
