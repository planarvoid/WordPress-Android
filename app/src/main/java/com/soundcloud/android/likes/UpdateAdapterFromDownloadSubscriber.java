package com.soundcloud.android.likes;

import com.soundcloud.android.events.OfflineSyncEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.propeller.PropertySet;

import java.util.Date;

class UpdateAdapterFromDownloadSubscriber extends DefaultSubscriber<OfflineSyncEvent> {

    private final ItemAdapter<PropertySet> adapter;

    UpdateAdapterFromDownloadSubscriber(ItemAdapter<PropertySet> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(OfflineSyncEvent offlineSyncEvent) {
        for (PropertySet item : adapter.getItems()){
            if (item.get(EntityProperty.URN).equals(offlineSyncEvent.getDownloadedItem())){
                item.put(TrackProperty.OFFLINE_DOWNLOADED_AT, new Date());
                adapter.notifyDataSetChanged();
            }
        }
    }
}
