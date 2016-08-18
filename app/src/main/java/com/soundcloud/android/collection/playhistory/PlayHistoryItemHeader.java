package com.soundcloud.android.collection.playhistory;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class PlayHistoryItemHeader extends PlayHistoryItem {

    static PlayHistoryItemHeader create(int trackCount) {
        return new AutoValue_PlayHistoryItemHeader(trackCount);
    }

    public abstract int trackCount();

    PlayHistoryItemHeader() {
        super(Kind.PlayHistoryHeader);
    }
}
