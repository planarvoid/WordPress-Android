package com.soundcloud.android.events;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.ads.VideoAdSource;
import com.soundcloud.android.playback.PlaybackStateTransition;

public class AdPlaybackErrorEvent extends LegacyTrackingEvent {

    public static final String KIND_FAIL_TO_BUFFER = "failToBuffer";

    private final String mediaType;
    private final String protocol;
    private final String playerType;
    private final String host;
    private final String format;
    private final int bitrate;

    private AdPlaybackErrorEvent(String kind,
                                 AdData adData,
                                 PlaybackStateTransition stateTransition,
                                 String format,
                                 int bitrate,
                                 String cdnHost) {
        super(kind, System.currentTimeMillis());
        this.mediaType = adData instanceof VideoAd ? "video" : "audio";
        this.protocol = protocolForStateTransition(stateTransition);
        this.playerType = playerTypeForStateTransition(stateTransition);
        this.host = cdnHost;
        this.bitrate = bitrate;
        this.format = format;
    }

    public static AdPlaybackErrorEvent failToBuffer(AdData adData,
                                                    PlaybackStateTransition stateTransition,
                                                    VideoAdSource videoSource) {
        final String format = videoSource.getType();
        final int bitrate = videoSource.getBitRateKbps();
        final String host = videoSource.getUrl();
        return new AdPlaybackErrorEvent(KIND_FAIL_TO_BUFFER, adData, stateTransition, format, bitrate, host);
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getPlayerType() {
        return playerType;
    }

    public int getBitrate() {
        return bitrate;
    }

    public String getFormat() {
        return format;
    }

    public String getHost() {
        return host;
    }

    public long getTimestamp() {
        return timestamp;
    }

    private static String playerTypeForStateTransition(PlaybackStateTransition stateTransition) {
        return stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE);
    }

    private static String protocolForStateTransition(PlaybackStateTransition stateTransition) {
        return stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL);
    }
}
