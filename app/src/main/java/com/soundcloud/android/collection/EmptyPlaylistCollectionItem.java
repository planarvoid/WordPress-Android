package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class EmptyPlaylistCollectionItem extends CollectionItem {

    static EmptyPlaylistCollectionItem create() {
        return new AutoValue_EmptyPlaylistCollectionItem(CollectionItem.TYPE_PLAYLIST_EMPTY);
    }

}
