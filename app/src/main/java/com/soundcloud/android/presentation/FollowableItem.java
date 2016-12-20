package com.soundcloud.android.presentation;

import com.soundcloud.android.model.Entity;

public interface FollowableItem extends Entity {
    FollowableItem updatedWithFollowing(boolean isFollowedByMe, int followingsCount);
}
