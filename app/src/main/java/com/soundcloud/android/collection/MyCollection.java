package com.soundcloud.android.collection;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;

import java.util.List;

public class MyCollection {

    private final LikesItem likes;
    private final List<PlaylistItem> likedAndPostedPlaylists;
    private final List<Urn> recentStations;
    private final boolean atLeastOneError;

    public MyCollection(LikesItem likes, List<PlaylistItem> likedAndPostedPlaylists,
                        List<Urn> recentStations, boolean atLeastOneError) {
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

    public List<Urn> getRecentStations() {
        return recentStations;
    }

    public boolean hasError() {
        return atLeastOneError;
    }
}
