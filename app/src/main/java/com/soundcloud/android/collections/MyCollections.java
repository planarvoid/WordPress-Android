package com.soundcloud.android.collections;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistItem;

import java.util.List;

public class MyCollections {

    private final List<Urn> likes;
    private final List<PlaylistItem> likedAndPostedPlaylists;

    public MyCollections(List<Urn> likes, List<PlaylistItem> likedAndPostedPlaylists) {
        this.likes = likes;
        this.likedAndPostedPlaylists = likedAndPostedPlaylists;
    }

    public List<PlaylistItem> getPlaylistItems() {
        return likedAndPostedPlaylists;
    }

    public List<Urn> getLikes() {
        return likes;
    }
}
