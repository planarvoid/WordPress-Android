package com.soundcloud.android.events;

public class PlaybackProgressEvent {
    long progress;
    long duration;

    public PlaybackProgressEvent(long progress, long duration) {
        this.progress = progress;
        this.duration = duration;
    }

    public long getDuration() {
        return duration;
    }

    public long getProgress() {
        return progress;
    }

    public float getProgressProportion() {
        return ((float) progress) / duration;
    }
}
