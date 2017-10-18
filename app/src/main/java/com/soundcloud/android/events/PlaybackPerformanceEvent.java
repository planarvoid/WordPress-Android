package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.Nullable;

@AutoValue
public abstract class PlaybackPerformanceEvent {

    public static final int METRIC_TIME_TO_PLAY = 0;
    public static final int METRIC_TIME_TO_PLAYLIST = 1;
    public static final int METRIC_TIME_TO_BUFFER = 2;
    public static final int METRIC_TIME_TO_SEEK = 3;
    public static final int METRIC_FRAGMENT_DOWNLOAD_RATE = 4;
    public static final int METRIC_TIME_TO_LOAD = 5;
    public static final int METRIC_CACHE_USAGE_PERCENT = 6;
    public static final int METRIC_UNINTERRUPTED_PLAYTIME_MS = 7;

    public static final String EVENT_NAME = "audio_performance";

    public abstract long timestamp();

    public abstract int metric();

    public abstract long metricValue();

    public abstract String format();

    public abstract int bitrate();

    public abstract String playbackProtocol();

    public abstract String playerType();

    @Nullable
    public abstract String cdnHost();

    public abstract Optional<String> details();

    public static Builder builder() {
        return new AutoValue_PlaybackPerformanceEvent.Builder()
                .timestamp(System.currentTimeMillis())
                .details(Optional.absent());
    }

    public static Builder timeToPlay() {
        return builder().metric(METRIC_TIME_TO_PLAY);
    }

    public static Builder timeToPlaylist() {
        return builder().metric(METRIC_TIME_TO_PLAYLIST);
    }

    public static Builder timeToBuffer() {
        return builder().metric(METRIC_TIME_TO_BUFFER);
    }

    public static Builder timeToSeek() {
        return builder().metric(METRIC_TIME_TO_SEEK);
    }

    public static Builder fragmentDownloadRate() {
        return builder().metric(METRIC_FRAGMENT_DOWNLOAD_RATE);
    }

    public static Builder timeToLoad() {
        return builder().metric(METRIC_TIME_TO_LOAD);
    }

    public static Builder cacheUsagePercent() {
        return builder().metric(METRIC_CACHE_USAGE_PERCENT);
    }

    public static Builder uninterruptedPlaytimeMs() {
        return builder().metric(METRIC_UNINTERRUPTED_PLAYTIME_MS);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder timestamp(long timestamp);

        public abstract Builder metric(int metric);

        public abstract Builder metricValue(long metricValue);

        public abstract Builder format(String format);

        public abstract Builder bitrate(int bitrate);

        public abstract Builder playbackProtocol(String playbackProtocol);

        public abstract Builder playerType(String playerType);

        public abstract Builder cdnHost(String cdnHost);

        public abstract Builder details(Optional<String> details);

        public abstract PlaybackPerformanceEvent build();
    }
}
