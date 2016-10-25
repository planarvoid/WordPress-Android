package com.soundcloud.android.tracks;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.adapters.PlayableViewItem;

public class UpdatePlayableAdapterSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {

    private final RecyclerItemAdapter adapter;

    public UpdatePlayableAdapterSubscriber(RecyclerItemAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(CurrentPlayQueueItemEvent event) {
        final PlayQueueItem playQueueItem = event.getCurrentPlayQueueItem();

        for (int i = 0; i < adapter.getItems().size(); i++) {
            final Object object = adapter.getItems().get(i);
            if ((object instanceof PlayableViewItem)) {
                final PlayableViewItem playableViewItem = ((PlayableViewItem) object);
                final boolean isCurrent = playableViewItem.getPlayableUrn().equals(playQueueItem.getUrnOrNotSet());

                if (playableViewItem.isPlaying() || isCurrent) {
                    playableViewItem.setIsPlaying(isCurrent);
                    adapter.notifyItemChanged(i);
                }
            }
        }
    }
}
