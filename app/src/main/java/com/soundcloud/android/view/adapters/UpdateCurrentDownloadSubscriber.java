package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.propeller.PropertySet;

public class UpdateCurrentDownloadSubscriber  extends DefaultSubscriber<CurrentDownloadEvent> {
    private final ItemAdapter<PropertySet> adapter;

    public UpdateCurrentDownloadSubscriber(ItemAdapter<PropertySet> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final CurrentDownloadEvent event) {
        for (PropertySet item : adapter.getItems()) {
            final Urn urn = item.get(EntityProperty.URN);
            if (event.getTrackUrn().equals(urn)){
                final boolean isDownloading = event.getKind() == CurrentDownloadEvent.START;
                item.put(OfflineProperty.DOWNLOADING, isDownloading);
                adapter.notifyDataSetChanged();
                break;
            }
        }
    }
}
