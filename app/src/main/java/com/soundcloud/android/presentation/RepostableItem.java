package com.soundcloud.android.presentation;

import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.Entity;

public interface RepostableItem extends Entity {
    RepostableItem updatedWithRepost(RepostsStatusEvent.RepostStatus repostStatus);
}
