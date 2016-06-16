package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class ViewAllCollectionItem extends CollectionItem {

    static final int TYPE_PLAY_HISTORY = 0;
    static final int TYPE_RECENTLY_PLAYED = 1;

    public static CollectionItem forRecentlyPlayed() {
        return new AutoValue_ViewAllCollectionItem(CollectionItem.TYPE_VIEW_ALL, TYPE_RECENTLY_PLAYED);
    }

    static ViewAllCollectionItem forPlayHistory() {
        return new AutoValue_ViewAllCollectionItem(CollectionItem.TYPE_VIEW_ALL, TYPE_PLAY_HISTORY);
    }

    abstract int getTarget();

}
