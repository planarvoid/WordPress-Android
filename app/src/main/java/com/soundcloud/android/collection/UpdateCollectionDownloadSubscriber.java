package com.soundcloud.android.collection;

import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.rx.observers.DefaultSubscriber;

class UpdateCollectionDownloadSubscriber extends DefaultSubscriber<OfflineContentChangedEvent> {

    private final CollectionAdapter adapter;

    UpdateCollectionDownloadSubscriber(CollectionAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public void onNext(final OfflineContentChangedEvent event) {
        for (int position = 0; position < adapter.getItems().size(); position++) {
            final CollectionItem item = adapter.getItem(position);
            if (event.isLikedTrackCollection && item.getType() == CollectionItem.TYPE_PREVIEW && adapter.getItems().size() > position) {
                final PreviewCollectionItem previewCollectionItem = (PreviewCollectionItem) item;
                adapter.setItem(position, previewCollectionItem.updatedWithOfflineState(event.state));
                break;

            }
        }
        adapter.getRecentlyPlayedBucketRenderer().update(event);
    }

}
