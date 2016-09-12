package com.soundcloud.android.collection.playlists;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PlaylistCollectionRemoveFilterItem extends PlaylistCollectionItem {

    public static PlaylistCollectionRemoveFilterItem create() {
        return new AutoValue_PlaylistCollectionRemoveFilterItem(PlaylistCollectionItem.TYPE_REMOVE_FILTER);
    }
}
