package com.soundcloud.android.associations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

import java.util.Set;

@AutoValue
public abstract class FollowingStatuses {

    protected abstract Set<Urn> followings();

    public static FollowingStatuses create(Set<Urn> followings) {
        return new AutoValue_FollowingStatuses(followings);
    }

    public boolean isFollowed(Urn urn) {
        return followings().contains(urn);
    }
}