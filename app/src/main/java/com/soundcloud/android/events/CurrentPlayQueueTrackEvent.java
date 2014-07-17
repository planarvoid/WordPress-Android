package com.soundcloud.android.events;

import com.soundcloud.android.tracks.TrackUrn;

public class CurrentPlayQueueTrackEvent {
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

    public boolean wasPositionUpdate() {
        return kind == POSITION_CHANGED;
    }

    public boolean wasNewQueue() {
        return kind == NEW_QUEUE;
    }

    public TrackUrn getCurrentTrackUrn() {
        return currentTrackUrn;
    }
}
