package com.soundcloud.android.events;

public final class PlayerLifeCycleEvent implements Event {

    public static final int STATE_IDLE = 0;
    public static final int STATE_DESTROYED = 1;

    public static PlayerLifeCycleEvent forIdle() {
        return new PlayerLifeCycleEvent(STATE_IDLE);
    }

    public static PlayerLifeCycleEvent forDestroyed() {
        return new PlayerLifeCycleEvent(STATE_DESTROYED);
    }

    private final int mKind;

    private PlayerLifeCycleEvent(int kind) {
        mKind = kind;
    }

    @Override
    public int getKind() {
        return mKind;
    }
}
