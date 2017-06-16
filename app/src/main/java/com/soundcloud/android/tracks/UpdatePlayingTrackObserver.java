package com.soundcloud.android.tracks;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.view.adapters.PlayingTrackAware;

public class UpdatePlayingTrackObserver extends DefaultObserver<CurrentPlayQueueItemEvent> {
    private final PlayingTrackAware adapter;

    public UpdatePlayingTrackObserver(PlayingTrackAware adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(CurrentPlayQueueItemEvent event) {
        final PlayQueueItem playQueueItem = event.getCurrentPlayQueueItem();
        adapter.updateNowPlaying(playQueueItem.getUrnOrNotSet());
    }
}
