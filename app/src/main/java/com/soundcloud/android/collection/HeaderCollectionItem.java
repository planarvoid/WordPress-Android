package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.R;

@AutoValue
public abstract class HeaderCollectionItem extends CollectionItem {

    public static CollectionItem forRecentlyPlayed() {
        return new AutoValue_HeaderCollectionItem(CollectionItem.TYPE_HEADER,
                                                  R.string.collections_recently_played_header,
                                                  false);
    }

    static HeaderCollectionItem forPlaylists() {
        return new AutoValue_HeaderCollectionItem(CollectionItem.TYPE_HEADER,
                                                  R.string.collections_playlists_header,
                                                  true);
    }

    static HeaderCollectionItem forPlayHistory() {
        return new AutoValue_HeaderCollectionItem(CollectionItem.TYPE_HEADER,
                                                  R.string.collections_play_history_header,
                                                  false);
    }

    abstract int getTitleResId();

    abstract boolean isWithFilterOptions();

}
