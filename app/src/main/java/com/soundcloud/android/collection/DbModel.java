package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.collection.playhistory.PlayHistoryModel;

public final class DbModel {

    @SuppressWarnings({"PMD.AbstractClassWithoutAnyMethod"})
    @AutoValue
    public abstract static class PlayHistory implements PlayHistoryModel {
        public static final Factory<PlayHistory> FACTORY = new Factory<>(AutoValue_DbModel_PlayHistory::new);
    }
}
