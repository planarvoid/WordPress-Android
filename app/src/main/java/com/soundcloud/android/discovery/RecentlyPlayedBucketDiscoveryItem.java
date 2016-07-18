package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedItem;

import java.util.List;

@AutoValue
public abstract class RecentlyPlayedBucketDiscoveryItem extends DiscoveryItem {

    RecentlyPlayedBucketDiscoveryItem() {
        super(Kind.RecentlyPlayedItem);
    }

    public static RecentlyPlayedBucketDiscoveryItem create(List<RecentlyPlayedItem> recentlyPlayedItems) {
        return new AutoValue_RecentlyPlayedBucketDiscoveryItem(recentlyPlayedItems);
    }

    public abstract List<RecentlyPlayedItem> getRecentlyPlayed();

}
