package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;

public interface PlayableViewItem {
    boolean updateNowPlaying(CurrentPlayQueueItemEvent event);
}
