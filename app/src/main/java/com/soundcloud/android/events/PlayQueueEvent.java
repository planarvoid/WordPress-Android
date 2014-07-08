package com.soundcloud.android.events;

import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;

public class PlayQueueEvent {

    public static final int NEW_QUEUE = 0;
    public static final int TRACK_CHANGE = 1;
    public static final int QUEUE_UPDATE = 2;

    private final int kind;
    private final TrackUrn currentTrackUrn;

    public PlayQueueEvent(int kind, TrackUrn currentTrackUrn) {
        this.kind = kind;
        this.currentTrackUrn = currentTrackUrn;
    }

    public int getKind() {
        return kind;
    }

    public static PlayQueueEvent fromNewQueue(TrackUrn currentTrackUrn) {
        return new PlayQueueEvent(NEW_QUEUE, currentTrackUrn);
    }

    @Deprecated
    public static PlayQueueEvent fromNewQueue(long currentTrackId) {
        return fromNewQueue(Urn.forTrack(currentTrackId));
    }

    public static PlayQueueEvent fromTrackChange(TrackUrn currentTrackUrn) {
        return new PlayQueueEvent(TRACK_CHANGE, currentTrackUrn);
    }

    @Deprecated
    public static PlayQueueEvent fromTrackChange(long currentTrackId) {
        return fromTrackChange(Urn.forTrack(currentTrackId));
    }

    public static PlayQueueEvent fromQueueUpdate(TrackUrn currentTrackUrn) {
        return new PlayQueueEvent(QUEUE_UPDATE, currentTrackUrn);
    }

    @Deprecated
    public static PlayQueueEvent fromQueueUpdate(long currentTrackId) {
        return fromQueueUpdate(Urn.forTrack(currentTrackId));
    }

    public TrackUrn getCurrentTrackUrn() {
        return currentTrackUrn;
    }

    public boolean isContentChange() {
        return kind == NEW_QUEUE || kind == QUEUE_UPDATE;
    }

}
