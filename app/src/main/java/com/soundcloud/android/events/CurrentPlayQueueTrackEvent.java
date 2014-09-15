package com.soundcloud.android.events;

import com.google.common.base.Objects;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;

public final class CurrentPlayQueueTrackEvent {
    private static final int NEW_QUEUE = 0;
    private static final int POSITION_CHANGED = 1;

    private final int kind;
    private final TrackUrn currentTrackUrn;

    private final PropertySet currentMetaData;

    private CurrentPlayQueueTrackEvent(int kind, TrackUrn currentTrackUrn, PropertySet currentMetaData) {
        this.kind = kind;
        this.currentTrackUrn = currentTrackUrn;
        this.currentMetaData = currentMetaData;
    }

    public static CurrentPlayQueueTrackEvent fromNewQueue(TrackUrn trackUrn) {
        return fromNewQueue(trackUrn, PropertySet.create());
    }

    public static CurrentPlayQueueTrackEvent fromNewQueue(TrackUrn trackUrn, PropertySet metaData) {
        return new CurrentPlayQueueTrackEvent(NEW_QUEUE, trackUrn, metaData);
    }

    public static CurrentPlayQueueTrackEvent fromPositionChanged(TrackUrn trackUrn) {
            return fromPositionChanged(trackUrn, PropertySet.create());
    }

    public static CurrentPlayQueueTrackEvent fromPositionChanged(TrackUrn trackUrn, PropertySet metaData) {
        return new CurrentPlayQueueTrackEvent(POSITION_CHANGED, trackUrn, metaData);
    }

    public int getKind() {
        return kind;
    }

    public boolean wasNewQueue() {
        return kind == NEW_QUEUE;
    }

    public TrackUrn getCurrentTrackUrn() {
        return currentTrackUrn;
    }

    public PropertySet getCurrentMetaData() {
        return currentMetaData;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CurrentPlayQueueTrackEvent) {
            CurrentPlayQueueTrackEvent event = (CurrentPlayQueueTrackEvent) o;
            return event.getKind() == kind && event.getCurrentTrackUrn().equals(currentTrackUrn);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(kind, currentTrackUrn);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("kind", kind == NEW_QUEUE ? "NEW_QUEUE" : "POSITION_CHANGED").toString();
    }
}
