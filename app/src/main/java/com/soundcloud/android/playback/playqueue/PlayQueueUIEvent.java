package com.soundcloud.android.playback.playqueue;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PlayQueueUIEvent {

    private final static int DISPLAY = 0;
    private final static int HIDE = 1;

    public static PlayQueueUIEvent createDisplayEvent() {
        return new AutoValue_PlayQueueUIEvent(DISPLAY);
    }

    public static PlayQueueUIEvent createHideEvent() {
        return new AutoValue_PlayQueueUIEvent(HIDE);
    }

    public abstract int getKind();

    public boolean isHideEvent() {
        return getKind() == HIDE;
    }

    public boolean isDisplayEvent() {
        return getKind() == DISPLAY;
    }

}
