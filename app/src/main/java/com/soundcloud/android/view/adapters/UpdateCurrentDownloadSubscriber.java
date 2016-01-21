package com.soundcloud.android.view.adapters;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.presentation.ItemAdapter;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.PropertySet;

public class UpdateCurrentDownloadSubscriber  extends DefaultSubscriber<OfflineContentChangedEvent> {
    private final ItemAdapter<? extends ListItem> adapter;

    public UpdateCurrentDownloadSubscriber(ItemAdapter<? extends ListItem> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final OfflineContentChangedEvent event) {
        boolean changed = false;
        for (ListItem item : adapter.getItems()) {
            final Urn urn = item.getEntityUrn();
            if (event.entities.contains(urn)) {
                changed = true;
                item.update(PropertySet.from(OfflineProperty.OFFLINE_STATE.bind(event.state)));
            }
        }
        if (changed) {
            adapter.notifyDataSetChanged();
        }
    }
}
