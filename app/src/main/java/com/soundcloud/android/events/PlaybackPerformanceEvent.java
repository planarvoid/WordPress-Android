package com.soundcloud.android.events;

public final class PlaybackPerformanceEvent {

    public static final int METRIC_TIME_TO_PLAY = 0;
    public static final int METRIC_TIME_TO_PLAYLIST = 1;
    public static final int METRIC_TIME_TO_BUFFER = 2;
    public static final int METRIC_TIME_TO_SEEK = 3;
    public static final int METRIC_FRAGMENT_DOWNLOAD_RATE = 4;

    private final long timestamp;
    private final int metric;
    private final long metricValue;
    private final Protocol protocol;
    private final PlayerType playerType;
    private final String uri;

    public static enum PlayerType {
        SKIPPY("Skippy"), MEDIA_PLAYER("MediaPlayer");

        private final String value;

        PlayerType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static enum Protocol {
        HLS("hls"), HTTPS("https");

        private final String value;

        Protocol(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private PlaybackPerformanceEvent(int metric, long value, Protocol protocol, PlayerType playerType, String uri) {
        this.metric = metric;
        this.metricValue = value;
        this.timestamp = System.currentTimeMillis();
        this.protocol = protocol;
        this.playerType = playerType;
        this.uri = uri;
    }

    public static PlaybackPerformanceEvent timeToPlay(long value, Protocol protocol, PlayerType playerType, String uri) {
        return new PlaybackPerformanceEvent(METRIC_TIME_TO_PLAY, value, protocol, playerType, uri);
    }

    public static PlaybackPerformanceEvent timeToPlaylist(long value, Protocol protocol, PlayerType playerType, String uri) {
        return new PlaybackPerformanceEvent(METRIC_TIME_TO_PLAYLIST, value, protocol, playerType, uri);
    }

    public static PlaybackPerformanceEvent timeToBuffer(long value, Protocol protocol, PlayerType playerType, String uri) {
        return new PlaybackPerformanceEvent(METRIC_TIME_TO_BUFFER, value, protocol, playerType, uri);
    }

    public static PlaybackPerformanceEvent timeToSeek(long value, Protocol protocol, PlayerType playerType, String uri) {
        return new PlaybackPerformanceEvent(METRIC_TIME_TO_SEEK, value, protocol, playerType, uri);
    }

    public static PlaybackPerformanceEvent fragmentDownloadRate(long value, Protocol protocol, PlayerType playerType, String uri) {
        return new PlaybackPerformanceEvent(METRIC_FRAGMENT_DOWNLOAD_RATE, value, protocol, playerType, uri);
    }

    public int getMetric() {
        return metric;
    }

    public long getMetricValue() {
        return metricValue;
    }

    public long getTimeStamp() {
        return timestamp;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public PlayerType getPlayerType() {
        return playerType;
    }

    public String getUri() {
        return uri;
    }
}
