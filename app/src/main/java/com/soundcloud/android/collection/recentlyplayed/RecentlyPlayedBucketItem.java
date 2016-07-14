package com.soundcloud.android.collection.recentlyplayed;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.collection.CollectionItem;

import java.util.List;

@AutoValue
public abstract class RecentlyPlayedBucketItem extends CollectionItem {

    public static RecentlyPlayedBucketItem create(List<RecentlyPlayedItem> recentlyPlayed) {
        return new AutoValue_RecentlyPlayedBucketItem(CollectionItem.TYPE_RECENTLY_PLAYED_BUCKET, recentlyPlayed);
    }

    abstract List<RecentlyPlayedItem> getRecentlyPlayedItems();
}
