package com.soundcloud.android.view.adapters;

import com.soundcloud.android.events.CurrentDownloadEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.propeller.PropertySet;

public class UpdateCurrentDownloadSubscriber  extends DefaultSubscriber<CurrentDownloadEvent> {
    private final ItemAdapter<TrackItem> adapter;

    public UpdateCurrentDownloadSubscriber(ItemAdapter<TrackItem> adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final CurrentDownloadEvent event) {
        for (TrackItem item : adapter.getItems()) {
            final Urn urn = item.getEntityUrn();
            if (event.getTrackUrn().equals(urn)){
                final boolean isDownloading = event.getKind() == CurrentDownloadEvent.START;
                item.update(PropertySet.from(OfflineProperty.DOWNLOADING.bind(isDownloading)));
                adapter.notifyDataSetChanged();
                break;
            }
        }
    }
}
