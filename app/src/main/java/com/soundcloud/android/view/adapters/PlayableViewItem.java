package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;

public interface PlayableViewItem<ItemT> {
    ItemT updateNowPlaying(CurrentPlayQueueItemEvent event);
}
