package com.soundcloud.android.tracks;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.adapters.ReactiveItemAdapter;

public final class UpdatePlayingTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueTrackEvent> {
    private final ReactiveItemAdapter adapter;
    private final TrackItemPresenter trackPresenter;

    public UpdatePlayingTrackSubscriber(ReactiveItemAdapter adapter, TrackItemPresenter trackPresenter) {
        this.adapter = adapter;
        this.trackPresenter = trackPresenter;
    }

    @Override
    public void onNext(CurrentPlayQueueTrackEvent event) {
        trackPresenter.setPlayingTrack(event.getCurrentTrackUrn());
        adapter.notifyDataSetChanged();
    }
}
