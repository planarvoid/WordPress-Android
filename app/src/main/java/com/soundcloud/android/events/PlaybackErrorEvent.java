package com.soundcloud.android.events;

import com.soundcloud.android.playback.PlaybackProtocol;

public class PlaybackErrorEvent {

    public static final String BITRATE_128 = "128";
    public static final String FORMAT_MP3 = "mp3";

    private final String category;
    private final PlaybackProtocol protocol;
    private final String cdnUri;

    private final String format;
    private final String bitrate;
    private final long timestamp;

    public PlaybackErrorEvent(String category, PlaybackProtocol protocol, String cdnUri, String bitrate, String format){
        this.category = category;
        this.protocol = protocol;
        this.cdnUri = cdnUri;
        this.bitrate = bitrate;
        this.format = format;
        this.timestamp = System.currentTimeMillis();
    }

    public PlaybackErrorEvent(String category, PlaybackProtocol protocol, String cdnUri){
        this(category, protocol, cdnUri, BITRATE_128, FORMAT_MP3);
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

    public String getCdnUrl() {
        return cdnUri;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
