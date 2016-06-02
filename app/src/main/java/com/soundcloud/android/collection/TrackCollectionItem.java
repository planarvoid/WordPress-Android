package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackItem;

@AutoValue
abstract class TrackCollectionItem extends CollectionItem {

    static TrackCollectionItem create(TrackItem trackItem) {
        return new AutoValue_TrackCollectionItem(CollectionItem.TYPE_TRACK_ITEM, trackItem);
    }

    abstract TrackItem getTrackItem();

    @Override
    public Urn getUrn() {
        return getTrackItem().getUrn();
    }

}
