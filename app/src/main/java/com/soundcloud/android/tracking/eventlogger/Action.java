package com.soundcloud.android.tracking.eventlogger;

import java.util.Locale;

public enum Action {
    PLAY, STOP, CHECKPOINT;

    public String toApiName() {
        return this.name().toLowerCase(Locale.ENGLISH);
    }
}
