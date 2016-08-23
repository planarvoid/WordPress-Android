package com.soundcloud.android.collection.recentlyplayed;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class RecentlyPlayedHeader extends RecentlyPlayedItem {

    public static RecentlyPlayedHeader create(int contextCount) {
        return new AutoValue_RecentlyPlayedHeader(Kind.RecentlyPlayedHeader, contextCount);
    }

    abstract int contextCount();
}
