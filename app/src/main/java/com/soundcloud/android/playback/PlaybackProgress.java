package com.soundcloud.android.playback;

import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.SystemClockDateProvider;

import android.support.annotation.VisibleForTesting;

public class PlaybackProgress {
    private final DateProvider dateProvider;
    private final long createdAt;

    final long position;
    final long duration;

    public static PlaybackProgress empty() {
        return new PlaybackProgress(0, 0);
    }

    @VisibleForTesting
    public PlaybackProgress(long position, long duration, DateProvider dateProvider) {
        this.position = position;
        this.duration = duration;
        this.dateProvider = dateProvider;
        this.createdAt = dateProvider.getCurrentTime();
    }

    public PlaybackProgress(long position, long duration) {
        this(position, duration, new SystemClockDateProvider());
    }

    public boolean isEmpty() {
        return position == 0 && duration == 0;
    }

    public boolean isDurationValid() {
        return duration > 0;
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

    public long getTimeLeft() {
        return Math.max(duration - position, 0L);
    }

    public long getTimeSinceCreation() {
        return dateProvider.getCurrentTime() - createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            PlaybackProgress that = (PlaybackProgress) o;
            return createdAt == that.createdAt
                    && duration == that.duration
                    && position == that.position;
        }
    }

    @Override
    public int hashCode() {
        int result = (int) (position ^ (position >>> 32));
        result = 31 * result + (int) (duration ^ (duration >>> 32));
        result = 31 * result + (int) (createdAt ^ (createdAt >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "PlaybackProgress{" +
                "position=" + position +
                ", duration=" + duration +
                ", createdAt=" + createdAt +
                '}';
    }
}
