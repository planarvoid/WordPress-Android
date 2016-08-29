package com.soundcloud.android.view.adapters;

import com.soundcloud.android.playback.PlayQueueManager;

public interface RepeatableItemAdapter {
    void updateInRepeatMode(PlayQueueManager.RepeatMode repeatMode);
}
