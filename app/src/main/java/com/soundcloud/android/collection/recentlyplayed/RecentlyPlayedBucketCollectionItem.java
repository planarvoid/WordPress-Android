package com.soundcloud.android.collection.recentlyplayed;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.collection.CollectionItem;
import com.soundcloud.android.collection.RecentlyPlayedItem;

import java.util.List;

@AutoValue
public abstract class RecentlyPlayedBucketCollectionItem extends CollectionItem {

    public static RecentlyPlayedBucketCollectionItem create(List<RecentlyPlayedItem> recentlyPlayed) {
        return new AutoValue_RecentlyPlayedBucketCollectionItem(CollectionItem.TYPE_RECENTLY_PLAYED_BUCKET, recentlyPlayed);
    }

    abstract List<RecentlyPlayedItem> getRecentlyPlayedItems();
}
