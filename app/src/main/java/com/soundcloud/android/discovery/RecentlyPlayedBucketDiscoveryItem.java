package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedPlayableItem;

import java.util.List;

@AutoValue
public abstract class RecentlyPlayedBucketDiscoveryItem extends DiscoveryItem {

    RecentlyPlayedBucketDiscoveryItem() {
        super(Kind.RecentlyPlayedItem);
    }

    public static RecentlyPlayedBucketDiscoveryItem create(List<RecentlyPlayedPlayableItem> recentlyPlayedPlayableItems) {
        return new AutoValue_RecentlyPlayedBucketDiscoveryItem(recentlyPlayedPlayableItems);
    }

    public abstract List<RecentlyPlayedPlayableItem> getRecentlyPlayed();

}
