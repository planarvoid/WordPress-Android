package com.soundcloud.android.collection.playhistory;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.tracks.TrackItem;

@AutoValue
abstract class PlayHistoryItemTrack extends PlayHistoryItem {

    static PlayHistoryItemTrack create(TrackItem trackItem) {
        return new AutoValue_PlayHistoryItemTrack(Kind.PlayHistoryTrack, trackItem);
    }

    public abstract TrackItem trackItem();

}
