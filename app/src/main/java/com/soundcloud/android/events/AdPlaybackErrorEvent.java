package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.ads.VideoAdSource;
import com.soundcloud.android.playback.PlaybackConstants;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class AdPlaybackErrorEvent extends NewTrackingEvent {
    private static final String ERROR_NAME = "failToBuffer";

    private static final String VIDEO_MEDIA_TYPE = "video";
    private static final String AUDIO_MEDIA_TYPE = "audio";

    public abstract String mediaType();

    public abstract String errorName();

    public abstract Optional<String> protocol();

    public abstract Optional<String> playerType();

    public abstract String host();

    public abstract String format();

    public abstract int bitrate();

    public static AdPlaybackErrorEvent failToBuffer(AdData adData,
                                                    PlaybackStateTransition stateTransition,
                                                    VideoAdSource videoSource) {
        final String format = getRichMediaFormatName(videoSource.getType());
        final int bitrate = videoSource.getBitRateKbps();
        final String host = videoSource.getUrl();
        final String mediaType = adData instanceof VideoAd ? VIDEO_MEDIA_TYPE : AUDIO_MEDIA_TYPE;
        final Optional<String> protocol = protocolForStateTransition(stateTransition);
        final Optional<String> playerType = playerTypeForStateTransition(stateTransition);
        return new AutoValue_AdPlaybackErrorEvent(defaultId(), defaultTimestamp(), Optional.absent(), mediaType, ERROR_NAME, protocol, playerType, host, format, bitrate);
    }

    @Override
    public AdPlaybackErrorEvent putReferringEvent(ReferringEvent referringEvent) {
        return new AutoValue_AdPlaybackErrorEvent(id(), timestamp(), Optional.of(referringEvent), mediaType(), errorName(), protocol(), playerType(), host(), format(), bitrate());
    }

    private static Optional<String> playerTypeForStateTransition(PlaybackStateTransition stateTransition) {
        return Optional.fromNullable(stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE));
    }

    private static Optional<String> protocolForStateTransition(PlaybackStateTransition stateTransition) {
        return Optional.fromNullable(stateTransition.getExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL));
    }

    private static String getRichMediaFormatName(String format) {
        switch (format) {
            case PlaybackConstants.MIME_TYPE_MP4:
                return "mp4";
            default:
                return format;
        }
    }
}
