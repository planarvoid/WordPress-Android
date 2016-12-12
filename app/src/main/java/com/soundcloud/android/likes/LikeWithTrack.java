package com.soundcloud.android.likes;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.tracks.TrackItem;

import java.util.Date;

@AutoValue
public abstract class LikeWithTrack {

    public static LikeWithTrack create(Like like, TrackItem trackItem) {
        return new AutoValue_LikeWithTrack(like, trackItem);
    }

    abstract Like like();

    abstract TrackItem trackItem();

}

