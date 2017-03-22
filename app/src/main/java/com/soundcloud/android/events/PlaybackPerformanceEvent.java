package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.PlaybackType;
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

    public enum EventName {
        RICH_MEDIA_EVENT_NAME("rich_media_stream_performance"),
        AUDIO_PERFORMANCE("audio_performance");
        private final String key;

        EventName(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public abstract long timestamp();

    public abstract int metric();

    public abstract long metricValue();

    public abstract String format();

    public abstract int bitrate();

    public abstract PlaybackProtocol protocol();

    public abstract PlayerType playerType();

    @Nullable
    public abstract String cdnHost();

    public abstract ConnectionType connectionType();

    public abstract Urn userUrn();

    public abstract boolean isAd();

    public abstract boolean isVideoAd();

    public abstract Optional<String> details();

    public static Builder builder() {
        return new AutoValue_PlaybackPerformanceEvent.Builder()
                .timestamp(System.currentTimeMillis())
                .userUrn(Urn.NOT_SET)
                .isAd(false)
                .isVideoAd(false)
                .details(Optional.absent());
    }

    public static Builder timeToPlay(PlaybackType playbackType) {
        return builder().metric(METRIC_TIME_TO_PLAY)
                        .isAd(isAd(playbackType))
                        .isVideoAd(isVideoAd(playbackType));
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

    public static Builder uninterruptedPlaytimeMs(PlaybackType playbackType) {
        return builder().metric(METRIC_UNINTERRUPTED_PLAYTIME_MS)
                        .isAd(isAd(playbackType))
                        .isVideoAd(isVideoAd(playbackType));
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder timestamp(long timestamp);

        public abstract Builder metric(int metric);

        public abstract Builder metricValue(long metricValue);

        public abstract Builder format(String format);

        public abstract Builder bitrate(int bitrate);

        public abstract Builder protocol(PlaybackProtocol protocol);

        public abstract Builder playerType(PlayerType playerType);

        public abstract Builder cdnHost(String cdnHost);

        public abstract Builder connectionType(ConnectionType connectionType);

        public abstract Builder userUrn(Urn userUrn);

        public abstract Builder isAd(boolean isAd);

        public abstract Builder isVideoAd(boolean isVideoAd);

        public abstract Builder details(Optional<String> details);

        public abstract PlaybackPerformanceEvent build();
    }

    public EventName eventName() {
        return isAd() ? EventName.RICH_MEDIA_EVENT_NAME : EventName.AUDIO_PERFORMANCE;
    }

    private static boolean isAd(PlaybackType playbackType) {
        return playbackType == PlaybackType.VIDEO_AD || playbackType == PlaybackType.AUDIO_AD;
    }

    private static boolean isVideoAd(PlaybackType playbackType) {
        return playbackType == PlaybackType.VIDEO_AD;
    }


}
