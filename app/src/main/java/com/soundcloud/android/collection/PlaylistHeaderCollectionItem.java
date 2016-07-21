package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class PlaylistHeaderCollectionItem extends CollectionItem {

    static PlaylistHeaderCollectionItem create(int playlistCount) {
        return create(playlistCount, false);
    }

    static PlaylistHeaderCollectionItem create(int playlistCount, boolean withTopSpacing) {
        return new AutoValue_PlaylistHeaderCollectionItem(CollectionItem.TYPE_PLAYLIST_HEADER,
                                                          playlistCount,
                                                          withTopSpacing);
    }

    abstract int getPlaylistCount();

    abstract boolean withTopSeparator();

}
