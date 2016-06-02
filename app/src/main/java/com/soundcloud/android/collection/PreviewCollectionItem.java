package com.soundcloud.android.collection;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.java.collections.PropertySet;

import java.util.List;

@AutoValue
abstract class PreviewCollectionItem extends CollectionItem {

    static PreviewCollectionItem create(LikesItem likes, List<StationRecord> stations) {
        return new AutoValue_PreviewCollectionItem(CollectionItem.TYPE_PREVIEW, likes, stations);
    }

    abstract LikesItem getLikes();

    abstract List<StationRecord> getStations();

    @Override
    public ListItem update(PropertySet sourceSet) {
        getLikes().update(sourceSet);
        return this;
    }
}
