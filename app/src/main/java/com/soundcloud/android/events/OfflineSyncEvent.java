package com.soundcloud.android.events;

public class OfflineSyncEvent {

    public static final int IDLE = 0;
    public static final int START = 1;
    public static final int STOP = 2;

    private final int kind;

    public OfflineSyncEvent(int kind) {
        this.kind = kind;
    }

    public static OfflineSyncEvent idle() {
        return new OfflineSyncEvent(IDLE);
    }

    public static OfflineSyncEvent start() {
        return new OfflineSyncEvent(START);
    }

    public static OfflineSyncEvent stop() {
        return new OfflineSyncEvent(STOP);
    }

    public int getKind() {
        return kind;
    }
}
