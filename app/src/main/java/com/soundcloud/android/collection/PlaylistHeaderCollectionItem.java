package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class PlaylistHeaderCollectionItem extends CollectionItem {

    static PlaylistHeaderCollectionItem create() {
        return new AutoValue_PlaylistHeaderCollectionItem(CollectionItem.TYPE_PLAYLIST_HEADER);
    }

}
