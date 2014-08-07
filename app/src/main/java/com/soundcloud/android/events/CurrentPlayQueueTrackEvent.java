package com.soundcloud.android.events;

import com.google.common.base.Objects;
import com.soundcloud.android.tracks.TrackUrn;

public final class CurrentPlayQueueTrackEvent {
    private static final int NEW_QUEUE = 0;
    private static final int POSITION_CHANGED = 1;

    private final int kind;
    private final TrackUrn currentTrackUrn;

    private CurrentPlayQueueTrackEvent(TrackUrn currentTrackUrn, int kind) {
        this.kind = kind;
        this.currentTrackUrn = currentTrackUrn;
    }

    public static CurrentPlayQueueTrackEvent fromNewQueue(TrackUrn trackUrn) {
        return new CurrentPlayQueueTrackEvent(trackUrn, NEW_QUEUE);
    }

    public static CurrentPlayQueueTrackEvent fromPositionChanged(TrackUrn trackUrn) {
        return new CurrentPlayQueueTrackEvent(trackUrn, POSITION_CHANGED);
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
}
