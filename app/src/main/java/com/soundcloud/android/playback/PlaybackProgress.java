package com.soundcloud.android.playback;

import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.android.utils.SystemClockDateProvider;

import android.support.annotation.VisibleForTesting;

public class PlaybackProgress {
    private final DateProvider dateProvider;
    private final long createdAt;
    private final long position;
    private long duration;

    public static PlaybackProgress empty() {
        return new PlaybackProgress(0, 0);
    }

    public static PlaybackProgress withDuration(PlaybackProgress progress, long duration) {
        return new PlaybackProgress(progress.getPosition(), duration, progress.getCreatedAt(), new SystemClockDateProvider());
    }

    public PlaybackProgress(long position, long duration) {
        this(position, duration, new SystemClockDateProvider());
    }

    @VisibleForTesting
    public PlaybackProgress(long position, long duration, DateProvider dateProvider) {
        this(position, duration, dateProvider.getCurrentTime(), dateProvider);
    }

    private PlaybackProgress(long position, long duration, long createdAt, DateProvider dateProvider) {
        this.position = position;
        this.duration = duration;
        this.createdAt = createdAt;
        this.dateProvider = dateProvider;
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

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getPosition() {
        return position;
    }

    public boolean isPastFirstQuartile() {
        return isPastPercentile(0.25f);
    }

    public boolean isPastSecondQuartile() {
        return isPastPercentile(0.50f);
    }

    public boolean isPastThirdQuartile() {
        return isPastPercentile(0.75f);
    }

    private boolean isPastPercentile(float percentile) {
        return isDurationValid() && ((float) position / (float) duration >= percentile);
    }

    public boolean isPastPosition(long position) {
        return this.position > position;
    }

    public long getTimeSinceCreation() {
        return dateProvider.getCurrentTime() - createdAt;
    }

    public long getCreatedAt() {
        return createdAt;
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
