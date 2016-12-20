package com.soundcloud.android.presentation;

import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.model.Entity;

public interface LikeableItem extends Entity {
    LikeableItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus);
}
