package com.soundcloud.android.events;

public final class PlayerLifeCycleEvent {

    public static final int STATE_IDLE = 0;
    public static final int STATE_DESTROYED = 1;

    private final int mKind;

    public static PlayerLifeCycleEvent forIdle() {
        return new PlayerLifeCycleEvent(STATE_IDLE);
    }

    public static PlayerLifeCycleEvent forDestroyed() {
        return new PlayerLifeCycleEvent(STATE_DESTROYED);
    }

    private PlayerLifeCycleEvent(int kind) {
        mKind = kind;
    }

    public int getKind() {
        return mKind;
    }
}
