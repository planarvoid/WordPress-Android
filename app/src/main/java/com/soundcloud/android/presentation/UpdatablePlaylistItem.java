package com.soundcloud.android.presentation;

import com.soundcloud.android.model.Entity;

public interface UpdatablePlaylistItem extends Entity {
    UpdatablePlaylistItem updatedWithTrackCount(int trackCount);

    UpdatablePlaylistItem updatedWithMarkedForOffline(boolean markedForOffline);
}
