package com.soundcloud.android.tracks;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.presentation.ItemAdapter;

public final class UpdatePlayingTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueTrackEvent> {
    private final ItemAdapter adapter;
    private final TrackItemRenderer trackRenderer;

    public UpdatePlayingTrackSubscriber(ItemAdapter adapter, TrackItemRenderer trackRenderer) {
        this.adapter = adapter;
        this.trackRenderer = trackRenderer;
    }

    @Override
    public void onNext(CurrentPlayQueueTrackEvent event) {
        trackRenderer.setPlayingTrack(event.getCurrentTrackUrn());
        adapter.notifyDataSetChanged();
    }
}
