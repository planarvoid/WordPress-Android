package com.soundcloud.android.events;

public class PlayQueueEvent {

    public static final int QUEUE_CHANGE = 0;
    public static final int TRACK_CHANGE = 1;
    public static final int RELATED_TRACKS_CHANGE = 2;

    private final int kind;

    public PlayQueueEvent(int kind) {
        this.kind = kind;
    }

    public int getKind() {
        return kind;
    }

    public static PlayQueueEvent fromQueueChange() {
        return new PlayQueueEvent(QUEUE_CHANGE);
    }

    public static PlayQueueEvent fromTrackChange() {
        return new PlayQueueEvent(TRACK_CHANGE);
    }

    public static PlayQueueEvent fromRelatedTracksChange() {
        return new PlayQueueEvent(RELATED_TRACKS_CHANGE);
    }

}
