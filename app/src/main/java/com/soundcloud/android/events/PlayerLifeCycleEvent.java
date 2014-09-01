package com.soundcloud.android.events;

public final class PlayerLifeCycleEvent {

    public static final int STATE_STOPPED = 0;
    public static final int STATE_DESTROYED = 1;
    public static final int STATE_CREATED = 2;
    public static final int STATE_STARTED = 3;

    private final int kind;

    public static PlayerLifeCycleEvent forStarted() {
        return new PlayerLifeCycleEvent(STATE_STARTED);
    }

    public static PlayerLifeCycleEvent forCreated() {
        return new PlayerLifeCycleEvent(STATE_CREATED);
    }

    public static PlayerLifeCycleEvent forStopped() {
        return new PlayerLifeCycleEvent(STATE_STOPPED);
    }

    public static PlayerLifeCycleEvent forDestroyed() {
        return new PlayerLifeCycleEvent(STATE_DESTROYED);
    }

    private PlayerLifeCycleEvent(int kind) {
        this.kind = kind;
    }

    public int getKind() {
        return kind;
    }

    public boolean isServiceRunning() {
        return kind == STATE_CREATED || kind == STATE_STARTED;
    }
}
