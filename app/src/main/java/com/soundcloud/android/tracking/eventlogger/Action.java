package com.soundcloud.android.tracking.eventlogger;

public enum Action {
    PLAY, STOP, CHECKPOINT;

    public String toApiName() {
        return this.name().toLowerCase();
    }
}
