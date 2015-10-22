package com.soundcloud.android.tracks;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.adapters.NowPlayingAdapter;

public final class UpdatePlayingTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {
    private final NowPlayingAdapter adapter;

    public UpdatePlayingTrackSubscriber(NowPlayingAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(CurrentPlayQueueItemEvent event) {
        final PlayQueueItem playQueueItem = event.getCurrentPlayQueueItem();
        adapter.updateNowPlaying(playQueueItem.getUrnOrNotSet());
    }
}
