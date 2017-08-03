package com.soundcloud.android.tracks;

import com.google.auto.factory.AutoFactory;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.view.adapters.PlayableViewItem;

import java.util.List;

@AutoFactory(allowSubclasses = true)
public class UpdatePlayableAdapterObserver extends DefaultObserver<CurrentPlayQueueItemEvent> {

    private final RecyclerItemAdapter adapter;

    public UpdatePlayableAdapterObserver(RecyclerItemAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(CurrentPlayQueueItemEvent event) {
        List items = adapter.getItems();
        for (int i = 0; i < items.size(); i++) {
            final Object object = items.get(i);
            if (object instanceof PlayableViewItem) {
                Object updatedObject = ((PlayableViewItem) object).updateNowPlaying(event);
                items.set(i, updatedObject);
                if (!object.equals(updatedObject)) {
                    adapter.notifyItemChanged(i);
                }
            }
        }
    }
}
