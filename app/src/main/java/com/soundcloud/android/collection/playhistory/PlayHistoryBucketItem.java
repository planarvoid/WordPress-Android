package com.soundcloud.android.collection.playhistory;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.collection.CollectionItem;
import com.soundcloud.android.tracks.TrackItem;

import java.util.List;

@AutoValue
public abstract class PlayHistoryBucketItem extends CollectionItem {

    public static PlayHistoryBucketItem create(List<TrackItem> listeningHistory) {
        return new AutoValue_PlayHistoryBucketItem(CollectionItem.TYPE_PLAY_HISTORY_BUCKET, listeningHistory);
    }

    abstract List<TrackItem> getListeningHistory();
}
