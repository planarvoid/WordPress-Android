package com.soundcloud.android.events;

public final class PlaybackPerformanceEvent {


    public static final int METRIC_TIME_TO_PLAY = 0;
    public static final int METRIC_TIME_TO_PLAYLIST = 1;
    public static final int METRIC_TIME_TO_BUFFER = 2;
    public static final int METRIC_TIME_TO_SEEK = 3;
    public static final int METRIC_FRAGMENT_DOWNLOAD_RATE = 4;

    public enum ConnectionType {

        TWO_G("2G"),
        THREE_G("3G"),
        FOUR_G("4g"),
        WIFI("wifi"),
        UNKNOWN("unknown");
        private final String value;

        ConnectionType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

    }

    public enum PlayerType {
        SKIPPY("Skippy"), MEDIA_PLAYER("MediaPlayer");

        private final String value;

        PlayerType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum Protocol {
        HLS("hls"), HTTPS("https");

        private final String value;

        Protocol(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private final long timestamp;
    private final int metric;
    private final long metricValue;
    private final Protocol protocol;
    private final PlayerType playerType;
    private final String uri;
    private final ConnectionType connectionType;

    private PlaybackPerformanceEvent(int metric, long value, Protocol protocol, PlayerType playerType,
                                     ConnectionType connectionType, String uri) {
        this.metric = metric;
        this.metricValue = value;
        this.timestamp = System.currentTimeMillis();
        this.protocol = protocol;
        this.playerType = playerType;
        this.uri = uri;
        this.connectionType = connectionType;
    }

    public static PlaybackPerformanceEvent timeToPlay(long value, Protocol protocol, PlayerType playerType,
                                                      ConnectionType connectionType, String uri) {
        return new PlaybackPerformanceEvent(METRIC_TIME_TO_PLAY, value, protocol, playerType, connectionType, uri);
    }

    public static PlaybackPerformanceEvent timeToPlaylist(long value, Protocol protocol, PlayerType playerType,
                                                          ConnectionType connectionType, String uri) {
        return new PlaybackPerformanceEvent(METRIC_TIME_TO_PLAYLIST, value, protocol, playerType, connectionType, uri);
    }

    public static PlaybackPerformanceEvent timeToBuffer(long value, Protocol protocol, PlayerType playerType,
                                                        ConnectionType connectionType, String uri) {
        return new PlaybackPerformanceEvent(METRIC_TIME_TO_BUFFER, value, protocol, playerType, connectionType, uri);
    }

    public static PlaybackPerformanceEvent timeToSeek(long value, Protocol protocol, PlayerType playerType,
                                                      ConnectionType connectionType, String uri) {
        return new PlaybackPerformanceEvent(METRIC_TIME_TO_SEEK, value, protocol, playerType, connectionType, uri);
    }

    public static PlaybackPerformanceEvent fragmentDownloadRate(long value, Protocol protocol, PlayerType playerType,
                                                                ConnectionType connectionType, String uri) {
        return new PlaybackPerformanceEvent(METRIC_FRAGMENT_DOWNLOAD_RATE, value, protocol, playerType, connectionType, uri);
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

    public ConnectionType getConnectionType() {
        return connectionType;
    }


}
