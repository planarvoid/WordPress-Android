package com.soundcloud.android.analytics.eventlogger;

import com.integralblue.httpresponsecache.compat.Charsets;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.utils.ScTextUtils;

import android.net.Uri;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;
public class EventLoggerParamsBuilder {

    @Inject
    public EventLoggerParamsBuilder() {
    }

    public String buildFromPlaybackEvent(PlaybackEvent playbackEvent) throws UnsupportedEncodingException {
        final Uri.Builder builder = new Uri.Builder();

        builder.appendQueryParameter("ts", String.valueOf(playbackEvent.getTimeStamp()));
        builder.appendQueryParameter("action", playbackEvent.isPlayEvent() ? EventLoggerParams.Action.PLAY : EventLoggerParams.Action.STOP);
        builder.appendQueryParameter("duration", String.valueOf(playbackEvent.getTrack().duration));
        builder.appendQueryParameter("sound", URLEncoder.encode(Urn.forTrack(playbackEvent.getTrack().getId()).toString(), Charsets.UTF_8.name()));
        builder.appendQueryParameter("user", URLEncoder.encode(Urn.forUser(playbackEvent.getUserId()).toString(), Charsets.UTF_8.name()));


        TrackSourceInfo trackSourceInfo = playbackEvent.getTrackSourceInfo();

        if (trackSourceInfo.getIsUserTriggered()) {
            builder.appendQueryParameter(EventLoggerParams.Keys.TRIGGER, EventLoggerParams.Trigger.MANUAL);
        } else {
            builder.appendQueryParameter(EventLoggerParams.Keys.TRIGGER, EventLoggerParams.Trigger.AUTO);
        }
        builder.appendQueryParameter(EventLoggerParams.Keys.ORIGIN_URL, formatOriginUrl(trackSourceInfo.getOriginScreen()));

        if (trackSourceInfo.hasSource()) {
            builder.appendQueryParameter(EventLoggerParams.Keys.SOURCE, trackSourceInfo.getSource());
            builder.appendQueryParameter(EventLoggerParams.Keys.SOURCE_VERSION, trackSourceInfo.getSourceVersion());
        }

        if (trackSourceInfo.isFromPlaylist()) {
            builder.appendQueryParameter(EventLoggerParams.Keys.SET_ID, String.valueOf(trackSourceInfo.getPlaylistId()));
            builder.appendQueryParameter(EventLoggerParams.Keys.SET_POSITION, String.valueOf(trackSourceInfo.getPlaylistPosition()));
        }
        return builder.build().getQuery();
    }


    public String buildFromPlaybackPerformanceEvent(PlaybackPerformanceEvent eventData) {
        final Uri.Builder builder = new Uri.Builder();
        builder.appendQueryParameter("ts", String.valueOf(eventData.getTimeStamp()));
        builder.appendQueryParameter("latency", String.valueOf(eventData.getMetricValue()));
        builder.appendQueryParameter("protocol", eventData.getProtocol().getValue());
        builder.appendQueryParameter("player_type", eventData.getPlayerType().getValue());
        builder.appendQueryParameter("type", getPerformanceEventType(eventData.getMetric()));
        builder.appendQueryParameter("host", Uri.parse(eventData.getUri()).getHost());
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
                return "fragment-rate";
            default:
                throw new IllegalArgumentException("Unexpected metric type " + type);
        }
    }
}
