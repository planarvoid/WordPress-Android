package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.offline.OfflineState;

import java.util.List;

@AutoValue
abstract class LikesItem {

    abstract List<LikedTrackPreview> trackPreviews();
    abstract OfflineState offlineState();

    static LikesItem fromTrackPreviews(List<LikedTrackPreview> trackPreviews) {
        return new AutoValue_LikesItem(trackPreviews, OfflineState.NOT_OFFLINE);
    }

    static LikesItem create(List<LikedTrackPreview> trackPreviews, OfflineState offlineState) {
        return new AutoValue_LikesItem(trackPreviews, offlineState);
    }

    public LikesItem offlineState(OfflineState offlineState) {
        return new AutoValue_LikesItem(trackPreviews(), offlineState);
    }
}
