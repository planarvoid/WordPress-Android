package com.soundcloud.android.likes;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Association;
import com.soundcloud.android.tracks.TrackItem;

@AutoValue
public abstract class LikeWithTrack {

    public static LikeWithTrack create(Association like, TrackItem trackItem) {
        return new AutoValue_LikeWithTrack(like, trackItem);
    }

    public abstract Association like();

    public abstract TrackItem trackItem();

}

