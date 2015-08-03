package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;

public final class CurrentPlayQueueTrackEvent {
    private static final int NEW_QUEUE = 0;
    private static final int POSITION_CHANGED = 1;

    private final int kind;
    private final Urn currentTrackUrn;
    private final Urn collectionUrn;
    private final PropertySet currentMetaData;
    private final int position;

    private CurrentPlayQueueTrackEvent(int kind, Urn currentTrackUrn, Urn collectionUrn, PropertySet currentMetaData, int position) {
        this.kind = kind;
        this.currentTrackUrn = currentTrackUrn;
        this.collectionUrn = collectionUrn;
        this.currentMetaData = currentMetaData;
        this.position = position;
    }

    public static CurrentPlayQueueTrackEvent fromNewQueue(Urn trackUrn, Urn collectionUrn, int position) {
        return fromNewQueue(trackUrn, collectionUrn, PropertySet.create(), position);
    }

    public static CurrentPlayQueueTrackEvent fromNewQueue(Urn trackUrn, Urn collectionUrn, PropertySet metaData, int position) {
        return new CurrentPlayQueueTrackEvent(NEW_QUEUE, trackUrn, collectionUrn, metaData, position);
    }
    
    public static CurrentPlayQueueTrackEvent fromPositionChanged(Urn trackUrn, Urn collectionUrn, int position) {
            return fromPositionChanged(trackUrn, collectionUrn, PropertySet.create(), position);
    }

    public static CurrentPlayQueueTrackEvent fromPositionChanged(Urn trackUrn, Urn collectionUrn, PropertySet metaData, int position) {
        return new CurrentPlayQueueTrackEvent(POSITION_CHANGED, trackUrn, collectionUrn, metaData, position);
    }

    public int getKind() {
        return kind;
    }

    public Urn getCurrentTrackUrn() {
        return currentTrackUrn;
    }

    public Urn getCollectionUrn() { return collectionUrn; }

    public int getPosition() {
        return position;
    }

    public PropertySet getCurrentMetaData() {
        return currentMetaData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CurrentPlayQueueTrackEvent that = (CurrentPlayQueueTrackEvent) o;

        return MoreObjects.equal(kind, that.getKind())
                && MoreObjects.equal(currentTrackUrn, that.getCurrentTrackUrn())
                && MoreObjects.equal(collectionUrn, that.getCollectionUrn())
                && MoreObjects.equal(currentMetaData, that.getCurrentMetaData());
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(kind, currentTrackUrn, collectionUrn, currentMetaData);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("kind", kind == NEW_QUEUE ? "NEW_QUEUE" : "POSITION_CHANGED").toString();
    }
}
