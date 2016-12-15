package com.soundcloud.android.presentation;

import com.soundcloud.android.events.LikesStatusEvent;

public interface LikeableItem {
    ListItem updatedWithLike(LikesStatusEvent.LikeStatus likeStatus);
}
