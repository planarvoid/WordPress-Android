package com.soundcloud.android.collection.recentlyplayed;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class RecentlyPlayedEmpty extends RecentlyPlayedItem {

    public static RecentlyPlayedEmpty create() {
        return new AutoValue_RecentlyPlayedEmpty(Kind.RecentlyPlayedEmpty);
    }
}
