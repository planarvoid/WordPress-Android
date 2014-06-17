package com.soundcloud.android.events;

import android.os.SystemClock;

public class PlaybackProgress {
    final long position;
    final long duration;

    private final long createdAt;

    public static PlaybackProgress empty() {
        return new PlaybackProgress(0, 0);
    }

    public PlaybackProgress(long position, long duration) {
        this.position = position;
        this.duration = duration;
        this.createdAt = SystemClock.uptimeMillis();
    }

    public long getDuration() {
        return duration;
    }

    public long getPosition() {
        return position;
    }

    public float getProgressProportion() {
        if (duration == 0) {
            return 0.0f;
        } else {
            return ((float) position) / duration;
        }
    }

    public long getTimeLeft(){
        return duration - position;
    }

    public long getTimeSinceCreation(){
        return SystemClock.uptimeMillis() - createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlaybackProgress)) return false;

        PlaybackProgress that = (PlaybackProgress) o;

        if (createdAt != that.createdAt) return false;
        if (duration != that.duration) return false;
        if (position != that.position) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (position ^ (position >>> 32));
        result = 31 * result + (int) (duration ^ (duration >>> 32));
        result = 31 * result + (int) (createdAt ^ (createdAt >>> 32));
        return result;
    }
}
