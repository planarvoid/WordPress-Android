package com.soundcloud.android.events;

public class PlayQueueEvent {

    public static final int QUEUE_CHANGE = 0;
    public static final int TRACK_CHANGE = 1;

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
}
