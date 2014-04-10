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
    private final String protocol;
    private final String playerType;
    private final String uri;

    private PlaybackPerformanceEvent(int metric, long value, String protocol, String playerType, String uri) {
        this.metric = metric;
        this.metricValue = value;
        this.timestamp = System.currentTimeMillis();
        this.protocol = protocol;
        this.playerType = playerType;
        this.uri = uri;
    }

    public static PlaybackPerformanceEvent timeToPlay(long value, String protocol, String playerType, String uri) {
        return new PlaybackPerformanceEvent(METRIC_TIME_TO_PLAY, value, protocol, playerType, uri);
    }

    public static PlaybackPerformanceEvent timeToPlaylist(long value, String protocol, String playerType, String uri) {
        return new PlaybackPerformanceEvent(METRIC_TIME_TO_PLAYLIST, value, protocol, playerType, uri);
    }

    public static PlaybackPerformanceEvent timeToBuffer(long value, String protocol, String playerType, String uri) {
        return new PlaybackPerformanceEvent(METRIC_TIME_TO_BUFFER, value, protocol, playerType, uri);
    }

    public static PlaybackPerformanceEvent timeToSeek(long value, String protocol, String playerType, String uri) {
        return new PlaybackPerformanceEvent(METRIC_TIME_TO_SEEK, value, protocol, playerType, uri);
    }

    public static PlaybackPerformanceEvent fragmentDownloadRate(long value, String protocol, String playerType, String uri) {
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

    public String getProtocol() {
        return protocol;
    }

    public String getPlayerType() {
        return playerType;
    }

    public String getUri() {
        return uri;
    }
}
