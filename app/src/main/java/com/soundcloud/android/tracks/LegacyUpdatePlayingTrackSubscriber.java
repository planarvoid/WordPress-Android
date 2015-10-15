package com.soundcloud.android.tracks;


import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.presentation.ItemAdapter;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

@Deprecated // use UpdatePlayingTrackSubscriber instead
public final class LegacyUpdatePlayingTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueTrackEvent> {
    private final ItemAdapter adapter;
    private final TrackItemRenderer trackRenderer;

    public LegacyUpdatePlayingTrackSubscriber(ItemAdapter adapter, TrackItemRenderer trackRenderer) {
        this.adapter = adapter;
        this.trackRenderer = trackRenderer;
    }

    @Override
    public void onNext(CurrentPlayQueueTrackEvent event) {
        trackRenderer.setPlayingTrack(event.getCurrentTrackUrn());
        adapter.notifyDataSetChanged();
    }
}
