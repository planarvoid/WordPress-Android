package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class PlaylistRemoveFilterCollectionItem extends CollectionItem {

    static PlaylistRemoveFilterCollectionItem create() {
        return new AutoValue_PlaylistRemoveFilterCollectionItem(CollectionItem.TYPE_PLAYLIST_REMOVE_FILTER);
    }
}
