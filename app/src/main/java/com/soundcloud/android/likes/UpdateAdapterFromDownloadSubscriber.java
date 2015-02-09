package com.soundcloud.android.likes;

import com.soundcloud.android.events.OfflineContentEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.propeller.PropertySet;

import java.util.Date;

class UpdateAdapterFromDownloadSubscriber extends DefaultSubscriber<OfflineContentEvent> {

    private final ItemAdapter<PropertySet> adapter;

    UpdateAdapterFromDownloadSubscriber(ItemAdapter<PropertySet> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(OfflineContentEvent offlineContentEvent) {
        for (PropertySet item : adapter.getItems()) {
            if (item.get(EntityProperty.URN).equals(offlineContentEvent.getUrn())) {
                if (offlineContentEvent.getKind() == OfflineContentEvent.DOWNLOAD_FINISHED) {
                    item.put(TrackProperty.OFFLINE_DOWNLOADED_AT, new Date());
                    item.put(TrackProperty.OFFLINE_DOWNLOADING, false);
                } else {
                    item.put(TrackProperty.OFFLINE_DOWNLOADING, true);
                }
                adapter.notifyDataSetChanged();
            }
        }
    }
}
