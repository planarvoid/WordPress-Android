package com.soundcloud.android.tracks;


import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.presentation.ItemAdapter;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

@Deprecated // use UpdatePlayingTrackSubscriber instead
public final class LegacyUpdatePlayingTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {
    private final ItemAdapter adapter;
    private final TrackItemRenderer trackRenderer;

    public LegacyUpdatePlayingTrackSubscriber(ItemAdapter adapter, TrackItemRenderer trackRenderer) {
        this.adapter = adapter;
        this.trackRenderer = trackRenderer;
    }

    @Override
    public void onNext(CurrentPlayQueueItemEvent event) {
        PlayQueueItem playQueueItem = event.getCurrentPlayQueueItem();
        trackRenderer.setPlayingTrack(playQueueItem.getUrnOrNotSet());
        adapter.notifyDataSetChanged();
    }
}
