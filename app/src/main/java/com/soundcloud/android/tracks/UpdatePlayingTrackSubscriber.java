package com.soundcloud.android.tracks;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.adapters.PlayingTrackAware;

public class UpdatePlayingTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {
    private final PlayingTrackAware adapter;

    public UpdatePlayingTrackSubscriber(PlayingTrackAware adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(CurrentPlayQueueItemEvent event) {
        final PlayQueueItem playQueueItem = event.getCurrentPlayQueueItem();
        adapter.updateNowPlaying(playQueueItem.getUrnOrNotSet());
    }
}
