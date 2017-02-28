package com.soundcloud.android.events;

import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.skippy.Skippy.SkippyMediaType;

public class PlaybackErrorEvent {
    public static final String EVENT_NAME = "audio_error";

    public static final String CATEGORY_OFFLINE_PLAY_UNAVAILABLE = "offline_play_unavailable";

    public static final int BITRATE_128 = 128000;

    private final String category;
    private final PlaybackProtocol protocol;
    private final String cdnHost;

    private final String format;
    private final ConnectionType connectionType;
    private final int bitrate;
    private final long timestamp;

    public PlaybackErrorEvent(String category, PlaybackProtocol protocol, String cdnHost, String format, int bitrate,
                              ConnectionType connectionType) {
        this.category = category;
        this.protocol = protocol;
        this.cdnHost = cdnHost;
        this.bitrate = bitrate;
        this.format = format;
        this.connectionType = connectionType;
        this.timestamp = System.currentTimeMillis();
    }

    public PlaybackErrorEvent(String category,
                              PlaybackProtocol protocol,
                              String cdnHost,
                              ConnectionType connectionType) {
        this(category, protocol, cdnHost, SkippyMediaType.MP3.name(), BITRATE_128, connectionType);
    }

    public String getCategory() {
        return category;
    }

    public PlaybackProtocol getProtocol() {
        return protocol;
    }

    public int getBitrate() {
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
