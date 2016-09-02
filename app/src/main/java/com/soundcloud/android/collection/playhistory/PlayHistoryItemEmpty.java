package com.soundcloud.android.collection.playhistory;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class PlayHistoryItemEmpty extends PlayHistoryItem {

    static PlayHistoryItemEmpty create() {
        return new AutoValue_PlayHistoryItemEmpty(Kind.PlayHistoryEmpty);
    }
}
