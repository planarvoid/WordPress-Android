package com.soundcloud.android.analytics.eventlogger;

import android.content.res.Resources;
import android.net.Uri;
import com.soundcloud.android.R;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.experiments.ExperimentOperations;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.utils.DeviceHelper;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Map;

public class EventLoggerUrlBuilder {

    private static final String ENDPOINT = "http://eventlogger.soundcloud.com";

    private final String appId;
    private final DeviceHelper deviceHelper;
    private final ExperimentOperations experimentOperations;

    @Inject
    public EventLoggerUrlBuilder(Resources resources, ExperimentOperations experimentOperations, DeviceHelper deviceHelper) {
        this.appId = resources.getString(R.string.app_id);
        this.experimentOperations = experimentOperations;
        this.deviceHelper = deviceHelper;
    }

    @SuppressWarnings("PMD.UnusedParameter")
    public String buildFromAdPlayback(PlaybackSessionEvent event) {
        return null;
    }

    public String buildFromPlaybackEvent(PlaybackSessionEvent playbackSessionEvent) {
        final Uri.Builder builder = buildUriForPath("audio");

        builder.appendQueryParameter(Parameters.TIMESTAMP.value(), String.valueOf(playbackSessionEvent.getTimeStamp()));
        builder.appendQueryParameter(Parameters.ACTION.value(), playbackSessionEvent.isPlayEvent() ? "play" : "stop");
        builder.appendQueryParameter(Parameters.DURATION.value(), String.valueOf(playbackSessionEvent.getDuration()));
        builder.appendQueryParameter(Parameters.SOUND.value(), playbackSessionEvent.getTrackUrn().toString());
        builder.appendQueryParameter(Parameters.USER.value(), playbackSessionEvent.getUserUrn().toString());

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
        return builder.build().toString();
    }

    public String buildFromPlaybackPerformanceEvent(PlaybackPerformanceEvent eventData) {
        final Uri.Builder builder = buildUriForPath("audio_performance");
        return builder.appendQueryParameter(Parameters.TIMESTAMP.value(), String.valueOf(eventData.getTimeStamp()))
                .appendQueryParameter(Parameters.LATENCY.value(), String.valueOf(eventData.getMetricValue()))
                .appendQueryParameter(Parameters.PROTOCOL.value(), eventData.getProtocol().getValue())
                .appendQueryParameter(Parameters.PLAYER_TYPE.value(), eventData.getPlayerType().getValue())
                .appendQueryParameter(Parameters.TYPE.value(), getPerformanceEventType(eventData.getMetric()))
                .appendQueryParameter(Parameters.HOST.value(), eventData.getCdnHost())
                .appendQueryParameter(Parameters.USER.value(), eventData.getUserUrn().toString())
                .appendQueryParameter(Parameters.CONNECTION_TYPE.value(), eventData.getConnectionType().getValue())
                .build().toString();
    }

    public String buildFromPlaybackErrorEvent(PlaybackErrorEvent eventData) {
        final Uri.Builder builder = buildUriForPath("audio_error");
        return builder.appendQueryParameter(Parameters.TIMESTAMP.value(), String.valueOf(eventData.getTimestamp()))
                .appendQueryParameter(Parameters.PROTOCOL.value(), eventData.getProtocol().getValue())
                .appendQueryParameter(Parameters.OS.value(), deviceHelper.getUserAgent())
                .appendQueryParameter(Parameters.BITRATE.value(), eventData.getBitrate())
                .appendQueryParameter(Parameters.FORMAT.value(), eventData.getFormat())
                .appendQueryParameter(Parameters.URL.value(), eventData.getCdnHost())
                .appendQueryParameter(Parameters.ERROR_CODE.value(), eventData.getCategory())
                .build().toString();
    }

    private Uri.Builder buildUriForPath(String path) {
        final Uri.Builder builder = Uri.parse(ENDPOINT).buildUpon().appendPath(path);
        builder.appendQueryParameter(Parameters.CLIENT_ID.value(), appId);
        builder.appendQueryParameter(Parameters.ANONYMOUS_ID.value(), deviceHelper.getUniqueDeviceID());
        return builder;
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

    static enum Parameters {
        CLIENT_ID("client_id"),
        ANONYMOUS_ID("anonymous_id"),
        TIMESTAMP("ts"),
        ACTION("action"),
        DURATION("duration"),
        SOUND("sound"),
        USER("user"),
        CONTEXT("context"),
        TRIGGER("trigger"),
        SOURCE("source"),
        SOURCE_VERSION("source_version"),
        PLAYLIST_ID("set_id"),
        PLAYLIST_POSITION("set_position"),
        LATENCY("latency"),
        PROTOCOL("protocol"),
        PLAYER_TYPE("player_type"),
        TYPE("type"),
        HOST("host"),
        CONNECTION_TYPE("connection_type"),
        OS("os"),
        BITRATE("bitrate"),
        FORMAT("format"),
        URL("url"),
        ERROR_CODE("errorCode");

        private final String value;

        Parameters(String value) {
            this.value = value;
        }

        public String value(){
            return value;
        }
    }
}
