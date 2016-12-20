package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

@AutoValue
public abstract class FollowingStatusEvent {

    public abstract Urn urn();

    public abstract boolean isFollowed();

    public abstract int followingsCount();

    public static FollowingStatusEvent createFollowed(Urn urn, int followingsCount) {
        return new AutoValue_FollowingStatusEvent(urn, true, followingsCount);
    }

    public static FollowingStatusEvent createUnfollowed(Urn urn, int followingsCount) {
        return new AutoValue_FollowingStatusEvent(urn, false, followingsCount);
    }
}
