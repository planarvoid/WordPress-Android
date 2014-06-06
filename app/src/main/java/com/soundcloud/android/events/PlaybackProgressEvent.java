package com.soundcloud.android.events;

public class PlaybackProgressEvent {
    final long progress;
    final long duration;

    public static PlaybackProgressEvent empty() {
        return new PlaybackProgressEvent(0, 0);
    }

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
        if (duration == 0) {
            return 0.0f;
        } else {
            return ((float) progress) / duration;
        }
    }
}
