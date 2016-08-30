package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.playback.PlaybackType;

public final class PlaybackPerformanceEvent {

    public static final int METRIC_TIME_TO_PLAY = 0;
    public static final int METRIC_TIME_TO_PLAYLIST = 1;
    public static final int METRIC_TIME_TO_BUFFER = 2;
    public static final int METRIC_TIME_TO_SEEK = 3;
    public static final int METRIC_FRAGMENT_DOWNLOAD_RATE = 4;
    public static final int METRIC_TIME_TO_LOAD = 5;
    public static final int METRIC_CACHE_USAGE_PERCENT = 6;
    public static final int METRIC_UNINTERRUPTED_PLAYTIME_MS = 7;

    private final long timestamp;
    private final int metric;
    private final long metricValue;
    private final String format;
    private final int bitrate;
    private final PlaybackProtocol protocol;
    private final PlayerType playerType;
    private final String cdnHost;
    private final ConnectionType connectionType;
    private final Urn userUrn;
    private PlaybackType playbackType;

    private PlaybackPerformanceEvent(int metric,
                                     long value,
                                     PlaybackProtocol protocol,
                                     PlayerType playerType,
                                     ConnectionType connectionType,
                                     String cdnHost,
                                     String format,
                                     int bitrate,
                                     Urn userUrn) {
        this.metric = metric;
        this.metricValue = value;
        this.format = format;
        this.bitrate = bitrate;
        this.timestamp = System.currentTimeMillis();
        this.protocol = protocol;
        this.playerType = playerType;
        this.cdnHost = cdnHost;
        this.connectionType = connectionType;
        this.userUrn = userUrn;
    }

    public static PlaybackPerformanceEvent uninterruptedPlaytimeMs(long value,
                                                                   PlaybackProtocol protocol,
                                                                   PlayerType playerType,
                                                                   ConnectionType connectionType,
                                                                   String cdnHost,
                                                                   String format,
                                                                   int bitRate,
                                                                   PlaybackType playbackType) {
        return new PlaybackPerformanceEvent(METRIC_UNINTERRUPTED_PLAYTIME_MS,
                                            value,
                                            protocol,
                                            playerType,
                                            connectionType,
                                            cdnHost,
                                            format,
                                            bitRate,
                                            Urn.NOT_SET).setPlaybackType(playbackType);
    }

    public static PlaybackPerformanceEvent cacheUsagePercent(long value,
                                                             PlaybackProtocol protocol,
                                                             PlayerType playerType,
                                                             ConnectionType connectionType,
                                                             String cdnHost,
                                                             String format,
                                                             int bitRate) {
        return new PlaybackPerformanceEvent(METRIC_CACHE_USAGE_PERCENT,
                                            value,
                                            protocol,
                                            playerType,
                                            connectionType,
                                            cdnHost,
                                            format,
                                            bitRate,
                                            Urn.NOT_SET);
    }

    public static PlaybackPerformanceEvent timeToPlay(long value,
                                                      PlaybackProtocol protocol,
                                                      PlayerType playerType,
                                                      ConnectionType connectionType,
                                                      String cdnHost,
                                                      String format,
                                                      int bitRate,
                                                      Urn urn,
                                                      PlaybackType playbackType) {
        return new PlaybackPerformanceEvent(METRIC_TIME_TO_PLAY,
                                            value,
                                            protocol,
                                            playerType,
                                            connectionType,
                                            cdnHost,
                                            format,
                                            bitRate,
                                            urn).setPlaybackType(playbackType);
    }

    public static PlaybackPerformanceEvent timeToPlaylist(long value,
                                                          PlaybackProtocol protocol,
                                                          PlayerType playerType,
                                                          ConnectionType connectionType,
                                                          String cdnHost,
                                                          String format,
                                                          int bitRate,
                                                          Urn urn) {
        return new PlaybackPerformanceEvent(METRIC_TIME_TO_PLAYLIST,
                                            value,
                                            protocol,
                                            playerType,
                                            connectionType,
                                            cdnHost,
                                            format,
                                            bitRate,
                                            urn);
    }

    public static PlaybackPerformanceEvent timeToBuffer(long value,
                                                        PlaybackProtocol protocol,
                                                        PlayerType playerType,
                                                        ConnectionType connectionType,
                                                        String cdnHost,
                                                        String format,
                                                        int bitRate,
                                                        Urn urn) {
        return new PlaybackPerformanceEvent(METRIC_TIME_TO_BUFFER,
                                            value,
                                            protocol,
                                            playerType,
                                            connectionType,
                                            cdnHost,
                                            format,
                                            bitRate,
                                            urn);
    }

    public static PlaybackPerformanceEvent timeToSeek(long value,
                                                      PlaybackProtocol protocol,
                                                      PlayerType playerType,
                                                      ConnectionType connectionType,
                                                      String cdnHost,
                                                      String format,
                                                      int bitRate,
                                                      Urn urn) {
        return new PlaybackPerformanceEvent(METRIC_TIME_TO_SEEK,
                                            value,
                                            protocol,
                                            playerType,
                                            connectionType,
                                            cdnHost,
                                            format,
                                            bitRate,
                                            urn);
    }

    public static PlaybackPerformanceEvent timeToLoad(long value,
                                                      PlaybackProtocol protocol,
                                                      PlayerType playerType,
                                                      ConnectionType connectionType,
                                                      String cdnHost,
                                                      String format,
                                                      int bitRate,
                                                      Urn urn) {
        return new PlaybackPerformanceEvent(METRIC_TIME_TO_LOAD,
                                            value,
                                            protocol,
                                            playerType,
                                            connectionType,
                                            cdnHost,
                                            format,
                                            bitRate,
                                            urn);
    }

    public static PlaybackPerformanceEvent fragmentDownloadRate(long value,
                                                                PlaybackProtocol protocol,
                                                                PlayerType playerType,
                                                                ConnectionType connectionType,
                                                                String cdnHost,
                                                                String format,
                                                                int bitRate,
                                                                Urn urn) {
        return new PlaybackPerformanceEvent(METRIC_FRAGMENT_DOWNLOAD_RATE,
                                            value,
                                            protocol,
                                            playerType,
                                            connectionType,
                                            cdnHost,
                                            format,
                                            bitRate,
                                            urn);
    }

    public PlaybackPerformanceEvent setPlaybackType(PlaybackType playbackType) {
        this.playbackType = playbackType;
        return this;
    }

    public boolean isAd(){
        return playbackType == PlaybackType.VIDEO_AD || playbackType == PlaybackType.AUDIO_AD;
    }

    public boolean isVideoAd() {
        return playbackType == PlaybackType.VIDEO_AD;
    }

    public int getMetric() {
        return metric;
    }

    public long getMetricValue() {
        return metricValue;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public PlaybackProtocol getProtocol() {
        return protocol;
    }

    public PlayerType getPlayerType() {
        return playerType;
    }

    public String getCdnHost() {
        return cdnHost;
    }

    public ConnectionType getConnectionType() {
        return connectionType;
    }

    public Urn getUserUrn() {
        return userUrn;
    }

    public String getFormat() {
        return format;
    }

    public int getBitrate() {
        return bitrate;
    }
}
