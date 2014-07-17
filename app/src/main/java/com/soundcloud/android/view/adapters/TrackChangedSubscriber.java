package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

import android.widget.BaseAdapter;

public final class TrackChangedSubscriber extends DefaultSubscriber<CurrentPlayQueueTrackEvent> {
    private final BaseAdapter adapter;
    private final TrackItemPresenter trackPresenter;

    public TrackChangedSubscriber(BaseAdapter adapter, TrackItemPresenter trackPresenter) {
        this.adapter = adapter;
        this.trackPresenter = trackPresenter;
    }

    @Override
    public void onNext(CurrentPlayQueueTrackEvent event) {
        trackPresenter.setPlayingTrack(event.getCurrentTrackUrn());
        adapter.notifyDataSetChanged();
    }
}
