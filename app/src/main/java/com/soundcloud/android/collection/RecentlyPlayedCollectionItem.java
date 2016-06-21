package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

@AutoValue
public abstract class RecentlyPlayedCollectionItem extends CollectionItem {

    public static RecentlyPlayedCollectionItem create(RecentlyPlayedItem item) {
        return new AutoValue_RecentlyPlayedCollectionItem(getRecentlyPlayedType(item), item);
    }

    private static int getRecentlyPlayedType(RecentlyPlayedItem item) {
        Urn urn = item.getUrn();

        if (urn.isPlaylist()) {
            return CollectionItem.TYPE_RECENTLY_PLAYED_PLAYLIST;
        } else if (urn.isStation()) {
            return CollectionItem.TYPE_RECENTLY_PLAYED_STATION;
        } else if (urn.isUser()) {
            return CollectionItem.TYPE_RECENTLY_PLAYED_PROFILE;
        } else {
            throw new IllegalArgumentException("Unexpected urn: " + urn);
        }
    }

    public abstract RecentlyPlayedItem getRecentlyPlayedItem();

    @Override
    boolean isSingleSpan() {
        return true;
    }
}
