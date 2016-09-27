package com.soundcloud.android.collection;

import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.collections.PropertySet;

class UpdateCollectionDownloadSubscriber extends DefaultSubscriber<OfflineContentChangedEvent> {

    private final CollectionAdapter adapter;

    UpdateCollectionDownloadSubscriber(CollectionAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final OfflineContentChangedEvent event) {
        for (CollectionItem item : adapter.getItems()) {
            if (event.isLikedTrackCollection && item.getType() == CollectionItem.TYPE_PREVIEW) {
                item.update(PropertySet.from(OfflineProperty.OFFLINE_STATE.bind(event.state)));
                adapter.notifyDataSetChanged();
                break;
            }
        }
        adapter.getRecentlyPlayedBucketRenderer().update(event);
    }

}
