package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

import android.widget.BaseAdapter;

public final class TrackChangedSubscriber extends DefaultSubscriber<PlayQueueEvent> {
    private final BaseAdapter adapter;
    private final TrackItemPresenter trackPresenter;

    public TrackChangedSubscriber(BaseAdapter adapter, TrackItemPresenter trackPresenter) {
        this.adapter = adapter;
        this.trackPresenter = trackPresenter;
    }

    @Override
    public void onNext(PlayQueueEvent event) {
        if (event.getKind() == PlayQueueEvent.NEW_QUEUE || event.getKind() == PlayQueueEvent.TRACK_CHANGE) {
            trackPresenter.setPlayingTrack(event.getCurrentTrackUrn());
            adapter.notifyDataSetChanged();
        }
    }
}
