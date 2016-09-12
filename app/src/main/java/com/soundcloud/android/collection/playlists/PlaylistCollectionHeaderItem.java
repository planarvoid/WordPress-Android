package com.soundcloud.android.collection.playlists;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PlaylistCollectionHeaderItem extends PlaylistCollectionItem {

    public static PlaylistCollectionHeaderItem create(int playlistCount) {
        return new AutoValue_PlaylistCollectionHeaderItem(PlaylistCollectionItem.TYPE_HEADER, playlistCount);
    }

    abstract int getPlaylistCount();

}
