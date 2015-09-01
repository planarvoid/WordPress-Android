package com.soundcloud.android.collections;

import com.soundcloud.android.playlists.PlaylistItem;

import java.util.List;

public class MyCollections {

    private final int likesCount;
    private final List<PlaylistItem> likedAndPostedPlaylists;

    public MyCollections(int likesCount, List<PlaylistItem> likedAndPostedPlaylists) {
        this.likesCount = likesCount;
        this.likedAndPostedPlaylists = likedAndPostedPlaylists;
    }

    public List<PlaylistItem> getPlaylistItems() {
        return likedAndPostedPlaylists;
    }

    public int getLikesCount() {
        return likesCount;
    }
}
