package com.soundcloud.android.events;

public class PlayQueueEvent {

    public static final int NEW_QUEUE = 0;
    public static final int TRACK_CHANGE = 1;
    public static final int QUEUE_UPDATE = 2;

    private final int kind;

    public PlayQueueEvent(int kind) {
        this.kind = kind;
    }

    public int getKind() {
        return kind;
    }

    public static PlayQueueEvent fromNewQueue() {
        return new PlayQueueEvent(NEW_QUEUE);
    }

    public static PlayQueueEvent fromTrackChange() {
        return new PlayQueueEvent(TRACK_CHANGE);
    }

    public static PlayQueueEvent fromQueueUpdate() {
        return new PlayQueueEvent(QUEUE_UPDATE);
    }

}
