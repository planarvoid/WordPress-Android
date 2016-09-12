package com.soundcloud.android.collection.playlists;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PlaylistCollectionEmptyPlaylistItem extends PlaylistCollectionItem {

    public static PlaylistCollectionEmptyPlaylistItem create() {
        return new AutoValue_PlaylistCollectionEmptyPlaylistItem(PlaylistCollectionItem.TYPE_EMPTY);
    }

}
