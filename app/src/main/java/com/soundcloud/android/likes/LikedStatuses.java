package com.soundcloud.android.likes;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import java.util.Set;

@AutoValue
public abstract class LikedStatuses {

    protected abstract Set<Urn> likes();

    public static LikedStatuses create(Set<Urn> likes) {
        return new AutoValue_LikedStatuses(likes);
    }

    public boolean isLiked(Urn urn) {
        return likes().contains(urn);
    }
}
