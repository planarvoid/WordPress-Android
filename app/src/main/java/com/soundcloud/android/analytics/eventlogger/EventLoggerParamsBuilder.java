package com.soundcloud.android.analytics.eventlogger;

import com.integralblue.httpresponsecache.compat.Charsets;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.experiments.ExperimentOperations;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.utils.ScTextUtils;

import android.net.Uri;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.Map;

public class EventLoggerParamsBuilder {

    private ExperimentOperations experimentOperations;
    private enum Parameters {
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
        CONNECTION_TYPE("connection_type");

        private String value;

        Parameters(String value) {
            this.value = value;
        }

        public String value(){
            return value;
        }
    }

    @Inject
    public EventLoggerParamsBuilder(ExperimentOperations experimentOperations) {
        this.experimentOperations = experimentOperations;
    }

    public String buildFromPlaybackEvent(PlaybackEvent playbackEvent) throws UnsupportedEncodingException {
        final Uri.Builder builder = new Uri.Builder();

        builder.appendQueryParameter(Parameters.TIMESTAMP.value(), String.valueOf(playbackEvent.getTimeStamp()));
        builder.appendQueryParameter(Parameters.ACTION.value(), playbackEvent.isPlayEvent() ? "play" : "stop");
        builder.appendQueryParameter(Parameters.DURATION.value(), String.valueOf(playbackEvent.getTrack().duration));
        builder.appendQueryParameter(Parameters.SOUND.value(), URLEncoder.encode(Urn.forTrack(playbackEvent.getTrack().getId()).toString(), Charsets.UTF_8.name()));
        builder.appendQueryParameter(Parameters.USER.value(), URLEncoder.encode(Urn.forUser(playbackEvent.getUserId()).toString(), Charsets.UTF_8.name()));

        for (Map.Entry<String, Integer> entry : experimentOperations.getTrackingParams().entrySet()) {
            builder.appendQueryParameter(entry.getKey(), String.valueOf(entry.getValue()));
        }

        TrackSourceInfo trackSourceInfo = playbackEvent.getTrackSourceInfo();

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
        return builder.build().getQuery();
    }


    public String buildFromPlaybackPerformanceEvent(PlaybackPerformanceEvent eventData) {
        final Uri.Builder builder = new Uri.Builder();
        builder.appendQueryParameter(Parameters.TIMESTAMP.value(), String.valueOf(eventData.getTimeStamp()));
        builder.appendQueryParameter(Parameters.LATENCY.value(), String.valueOf(eventData.getMetricValue()));
        builder.appendQueryParameter(Parameters.PROTOCOL.value(), eventData.getProtocol().getValue());
        builder.appendQueryParameter(Parameters.PLAYER_TYPE.value(), eventData.getPlayerType().getValue());
        builder.appendQueryParameter(Parameters.TYPE.value(), getPerformanceEventType(eventData.getMetric()));
        builder.appendQueryParameter(Parameters.HOST.value(), Uri.parse(eventData.getUri()).getHost());
        builder.appendQueryParameter(Parameters.CONNECTION_TYPE.value(), eventData.getConnectionType().getValue());
        return builder.build().getQuery();
    }

    private String formatOriginUrl(String originUrl) {
        try {
            return URLEncoder.encode(originUrl.toLowerCase(Locale.ENGLISH).replace(" ", "_"), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ScTextUtils.EMPTY_STRING;
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
