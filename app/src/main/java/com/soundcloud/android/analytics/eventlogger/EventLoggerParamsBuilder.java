package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.experiments.ExperimentOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.utils.DeviceHelper;

import android.net.Uri;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Map;

public class EventLoggerParamsBuilder {

    private final DeviceHelper deviceHelper;
    private final ExperimentOperations experimentOperations;

    @Inject
    public EventLoggerParamsBuilder(ExperimentOperations experimentOperations, DeviceHelper deviceHelper) {
        this.experimentOperations = experimentOperations;
        this.deviceHelper = deviceHelper;
    }

    public String buildFromPlaybackEvent(PlaybackSessionEvent playbackSessionEvent) throws UnsupportedEncodingException {
        final Uri.Builder builder = new Uri.Builder();

        builder.appendQueryParameter(Parameters.TIMESTAMP.value(), String.valueOf(playbackSessionEvent.getTimeStamp()));
        builder.appendQueryParameter(Parameters.ACTION.value(), playbackSessionEvent.isPlayEvent() ? "play" : "stop");
        builder.appendQueryParameter(Parameters.DURATION.value(), String.valueOf(playbackSessionEvent.getTrack().duration));
        builder.appendQueryParameter(Parameters.SOUND.value(), Urn.forTrack(playbackSessionEvent.getTrack().getId()).toString());
        builder.appendQueryParameter(Parameters.USER.value(), Urn.forUser(playbackSessionEvent.getUserId()).toString());

        for (Map.Entry<String, Integer> entry : experimentOperations.getTrackingParams().entrySet()) {
            builder.appendQueryParameter(entry.getKey(), String.valueOf(entry.getValue()));
        }

        TrackSourceInfo trackSourceInfo = playbackSessionEvent.getTrackSourceInfo();

        if (trackSourceInfo.getIsUserTriggered()) {
            builder.appendQueryParameter(Parameters.TRIGGER.value(), "manual");
        } else {
            builder.appendQueryParameter(Parameters.TRIGGER.value(), "auto");
        }
        builder.appendQueryParameter(Parameters.CONTEXT.value(), formatOriginUrl(trackSourceInfo.getOriginScreen()));

        if (trackSourceInfo.hasSource()) {
            builder.appendQueryParameter(Parameters.SOURCE.value(), trackSourceInfo.getSource());
            builder.appendQueryParameter(Parameters.SOURCE_VERSION.value(), trackSourceInfo.getSourceVersion());
        }

        if (trackSourceInfo.isFromPlaylist()) {
            builder.appendQueryParameter(Parameters.PLAYLIST_ID.value(), String.valueOf(trackSourceInfo.getPlaylistId()));
            builder.appendQueryParameter(Parameters.PLAYLIST_POSITION.value(), String.valueOf(trackSourceInfo.getPlaylistPosition()));
        }
        return builder.build().getEncodedQuery();
    }


    public String buildFromPlaybackPerformanceEvent(PlaybackPerformanceEvent eventData) {
        final Uri.Builder builder = new Uri.Builder();
        builder.appendQueryParameter(Parameters.TIMESTAMP.value(), String.valueOf(eventData.getTimeStamp()));
        builder.appendQueryParameter(Parameters.LATENCY.value(), String.valueOf(eventData.getMetricValue()));
        builder.appendQueryParameter(Parameters.PROTOCOL.value(), eventData.getProtocol().getValue());
        builder.appendQueryParameter(Parameters.PLAYER_TYPE.value(), eventData.getPlayerType().getValue());
        builder.appendQueryParameter(Parameters.TYPE.value(), getPerformanceEventType(eventData.getMetric()));
        builder.appendQueryParameter(Parameters.HOST.value(), Uri.parse(eventData.getCdnHost()).getHost());
        builder.appendQueryParameter(Parameters.CONNECTION_TYPE.value(), eventData.getConnectionType().getValue());
        return builder.build().getEncodedQuery();
    }

    public String buildFromPlaybackErrorEvent(PlaybackErrorEvent eventData) {
        final Uri.Builder builder = new Uri.Builder();
        builder.appendQueryParameter(Parameters.TIMESTAMP.value(), String.valueOf(eventData.getTimestamp()));
        builder.appendQueryParameter(Parameters.PROTOCOL.value(), eventData.getProtocol().getValue());
        builder.appendQueryParameter(Parameters.OS.value(), deviceHelper.getUserAgent());
        builder.appendQueryParameter(Parameters.BITRATE.value(), eventData.getBitrate());
        builder.appendQueryParameter(Parameters.FORMAT.value(), eventData.getFormat());
        builder.appendQueryParameter(Parameters.URL.value(), eventData.getCdnHost());
        builder.appendQueryParameter(Parameters.ERROR_CODE.value(), eventData.getCategory());
        return builder.build().getEncodedQuery();
    }

    private String formatOriginUrl(String originUrl) {
        return originUrl.toLowerCase(Locale.ENGLISH).replace(" ", "_");
    }

    private String getPerformanceEventType(int type) {
        switch (type) {
            case PlaybackPerformanceEvent.METRIC_TIME_TO_PLAY:
                return "play";
            case PlaybackPerformanceEvent.METRIC_TIME_TO_BUFFER:
                return "buffer";
            case PlaybackPerformanceEvent.METRIC_TIME_TO_PLAYLIST:
                return "playlist";
            case PlaybackPerformanceEvent.METRIC_TIME_TO_SEEK:
                return "seek";
            case PlaybackPerformanceEvent.METRIC_FRAGMENT_DOWNLOAD_RATE:
                return "fragmentRate";
            default:
                throw new IllegalArgumentException("Unexpected metric type " + type);
        }
    }

}
