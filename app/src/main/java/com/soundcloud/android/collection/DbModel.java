package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.collection.playhistory.PlayHistoryModel;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedModel;

public final class DbModel {

    @SuppressWarnings({"PMD.AbstractClassWithoutAnyMethod"})
    @AutoValue
    public abstract static class PlayHistory implements PlayHistoryModel {
        public static final Factory<PlayHistory> FACTORY = new Factory<>(AutoValue_DbModel_PlayHistory::new);
    }

    @SuppressWarnings({"PMD.AbstractClassWithoutAnyMethod"})
    @AutoValue
    public abstract static class RecentlyPlayed implements RecentlyPlayedModel {
        public static final Factory<RecentlyPlayed> FACTORY = new Factory<>(AutoValue_DbModel_RecentlyPlayed::new);
    }
}
