package com.soundcloud.android.view.adapters;

import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.OfflineItem;
import com.soundcloud.android.presentation.RecyclerItemAdapter;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.Iterables;

public class UpdateCurrentDownloadSubscriber extends DefaultSubscriber<OfflineContentChangedEvent> {
    private final RecyclerItemAdapter adapter;

    public UpdateCurrentDownloadSubscriber(RecyclerItemAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onNext(final OfflineContentChangedEvent event) {
        final Iterable<ListItem> filtered = Iterables.filter(adapter.getItems(), ListItem.class);
        for (ListItem item : filtered) {
            if (event.entities.contains(item.getUrn()) && item instanceof OfflineItem) {
                final int position = adapter.getItems().indexOf(item);
                final ListItem listItem = ((OfflineItem) item).updatedWithOfflineState(event.state);
                if (adapter.getItems().size() > position) {
                    adapter.getItems().set(position, listItem);
                    adapter.notifyItemChanged(position);
                }
            }
        }
    }
}
