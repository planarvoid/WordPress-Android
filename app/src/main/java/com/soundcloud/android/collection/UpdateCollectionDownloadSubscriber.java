package com.soundcloud.android.collection;

import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.PropertySet;

public class UpdateCollectionDownloadSubscriber extends DefaultSubscriber<OfflineContentChangedEvent> {

    private final CollectionAdapter adapter;

    public UpdateCollectionDownloadSubscriber(CollectionAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final OfflineContentChangedEvent event) {
        boolean changed = false;

        for (CollectionItem item : adapter.getItems()) {
            if (event.entities.contains(item.getEntityUrn())
                    || (event.isLikedTrackCollection && item.isCollectionPreview())) {
                changed = true;
                item.update(PropertySet.from(OfflineProperty.OFFLINE_STATE.bind(event.state)));
            }
        }

        if (changed) {
            adapter.notifyDataSetChanged();
        }
    }

}
