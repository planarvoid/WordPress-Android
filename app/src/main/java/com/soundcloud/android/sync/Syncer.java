package com.soundcloud.android.sync;

public abstract class Syncer {

    public enum Action {
        DEFAULT, HARD_REFRESH, APPEND
    }

    public enum Result {
        CHANGED, UNCHANGED
    }

    public abstract Result call(Action action);
}
