package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.presentation.ItemAdapter;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.propeller.PropertySet;

public class UpdateCurrentDownloadSubscriber  extends DefaultSubscriber<CurrentDownloadEvent> {
    private final ItemAdapter<? extends ListItem> adapter;

    public UpdateCurrentDownloadSubscriber(ItemAdapter<? extends ListItem> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final CurrentDownloadEvent event) {
        boolean changed = false;
        for (ListItem item : adapter.getItems()) {
            final Urn urn = item.getEntityUrn();
            if (event.entities.contains(urn)){
                changed = true;
                item.update(PropertySet.from(OfflineProperty.DOWNLOAD_STATE.bind(event.kind)));
            }
        }
        if (changed) {
            adapter.notifyDataSetChanged();
        }
    }
}
