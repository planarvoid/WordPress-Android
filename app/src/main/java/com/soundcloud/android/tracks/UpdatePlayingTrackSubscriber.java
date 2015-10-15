package com.soundcloud.android.tracks;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.adapters.NowPlayingAdapter;

public final class UpdatePlayingTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueTrackEvent> {
    private final NowPlayingAdapter adapter;

    public UpdatePlayingTrackSubscriber(NowPlayingAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(CurrentPlayQueueTrackEvent event) {
        adapter.updateNowPlaying(event.getCurrentTrackUrn());
    }
}
