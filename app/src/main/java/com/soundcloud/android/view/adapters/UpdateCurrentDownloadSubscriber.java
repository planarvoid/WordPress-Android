package com.soundcloud.android.view.adapters;

import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.presentation.ItemAdapter;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.java.collections.PropertySet;

public class UpdateCurrentDownloadSubscriber extends DefaultSubscriber<OfflineContentChangedEvent> {
    private final ItemAdapter adapter;

    public UpdateCurrentDownloadSubscriber(ItemAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onNext(final OfflineContentChangedEvent event) {
        boolean changed = false;
        final Iterable<ListItem> filtered = Iterables.filter(adapter.getItems(), ListItem.class);
        for (ListItem item : filtered) {
            if (event.entities.contains(item.getUrn())) {
                changed = true;
                item.update(PropertySet.from(OfflineProperty.OFFLINE_STATE.bind(event.state)));
            }
        }

        if (changed) {
            adapter.notifyDataSetChanged();
        }
    }
}
