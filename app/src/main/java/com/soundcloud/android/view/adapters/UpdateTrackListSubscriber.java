package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.TrackChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.presentation.UpdatableTrackItem;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.Iterables;

public final class UpdateTrackListSubscriber extends DefaultSubscriber<TrackChangedEvent> {
    private final RecyclerItemAdapter adapter;

    public UpdateTrackListSubscriber(RecyclerItemAdapter adapter) {
        this.adapter = adapter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onNext(final TrackChangedEvent event) {
        final Iterable<UpdatableTrackItem> filtered = Iterables.filter(adapter.getItems(), UpdatableTrackItem.class);
        for (UpdatableTrackItem item : filtered) {
            final Urn urn = item.getUrn();
            if (event.changeMap().containsKey(urn)) {
                final int position = adapter.getItems().indexOf(item);
                if (adapter.getItems().size() > position) {
                    adapter.getItems().set(position, item.updatedWithTrackItem(event.changeMap().get(item.getUrn())));
                    adapter.notifyItemChanged(position);
                }
            }
        }
    }
}
