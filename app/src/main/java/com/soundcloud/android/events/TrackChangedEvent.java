package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackItem;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@AutoValue
public abstract class TrackChangedEvent {
    public abstract Map<Urn, TrackItem> changeMap();

    public static TrackChangedEvent forUpdate(TrackItem trackItem) {
        return new AutoValue_TrackChangedEvent(Collections.singletonMap(trackItem.getUrn(), trackItem));
    }

    public static TrackChangedEvent forUpdate(Collection<TrackItem> trackItems) {
        final Map<Urn, TrackItem> changeSet = new HashMap<>(trackItems.size());
        for (TrackItem trackItem : trackItems) {
            changeSet.put(trackItem.getUrn(), trackItem);
        }
        return new AutoValue_TrackChangedEvent(changeSet);
    }
}
