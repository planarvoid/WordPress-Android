package com.soundcloud.android.likes;

import com.google.auto.value.AutoValue;

@AutoValue
abstract class TrackLikesHeaderItem extends TrackLikesItem {

    public static TrackLikesHeaderItem create() {
        return new AutoValue_TrackLikesHeaderItem(Kind.HeaderItem);
    }
}
