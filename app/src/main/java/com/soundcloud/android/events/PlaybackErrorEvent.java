package com.soundcloud.android.events;

import com.soundcloud.android.playback.PlaybackProtocol;

public class PlaybackErrorEvent {

    public static final String CATEGORY_OFFLINE_PLAY_UNAVAILABLE = "offline_play_unavailable";

    public static final String BITRATE_128 = "128";
    public static final String FORMAT_MP3 = "mp3";

    private final String category;
    private final PlaybackProtocol protocol;
    private final String cdnHost;

    private final String format;
    private final ConnectionType connectionType;
    private final String bitrate;
    private final long timestamp;

    public PlaybackErrorEvent(String category, PlaybackProtocol protocol, String cdnHost, String bitrate, String format,
                              ConnectionType connectionType){
        this.category = category;
        this.protocol = protocol;
        this.cdnHost = cdnHost;
        this.bitrate = bitrate;
        this.format = format;
        this.connectionType = connectionType;
        this.timestamp = System.currentTimeMillis();
    }

    public PlaybackErrorEvent(String category, PlaybackProtocol protocol, String cdnHost, ConnectionType connectionType){
        this(category, protocol, cdnHost, BITRATE_128, FORMAT_MP3, connectionType);
    }

    public String getCategory() {
        return category;
    }

    public PlaybackProtocol getProtocol() {
        return protocol;
    }

    public String getBitrate() {
        return bitrate;
    }

    public String getFormat() {
        return format;
    }

    public String getCdnHost() {
        return cdnHost;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public ConnectionType getConnectionType() {
        return connectionType;
    }
}
