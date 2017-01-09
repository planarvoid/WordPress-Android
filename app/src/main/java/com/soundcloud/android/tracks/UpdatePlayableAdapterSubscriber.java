package com.soundcloud.android.tracks;

import com.google.auto.factory.AutoFactory;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.adapters.PlayableViewItem;

@AutoFactory(allowSubclasses = true)
public class UpdatePlayableAdapterSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {

    private final RecyclerItemAdapter adapter;

    public UpdatePlayableAdapterSubscriber(RecyclerItemAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(CurrentPlayQueueItemEvent event) {
        boolean updated = false;
        for (int i = 0; i < adapter.getItems().size(); i++) {
            final Object object = adapter.getItems().get(i);
            if ((object instanceof PlayableViewItem)) {
                if (((PlayableViewItem) object).updateNowPlaying(event)) {
                    updated = true;
                }
            }
        }
        if (updated) {
            adapter.notifyDataSetChanged();
        }
    }
}
