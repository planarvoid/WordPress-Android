package com.soundcloud.android.analytics.localytics;

import com.google.common.base.Objects;
import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.analytics.AnalyticsProperties;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

public class LocalyticsAnalyticsProvider implements AnalyticsProvider {
    public final String TAG = "LocalyticsProvider";
    private LocalyticsSession mLocalyticsSession;

    public LocalyticsAnalyticsProvider(Context context) {
        this(new LocalyticsSession(context.getApplicationContext(),
                new AnalyticsProperties(context.getResources()).getLocalyticsAppKey()));
    }

    protected LocalyticsAnalyticsProvider(LocalyticsSession localyticsSession) {
        mLocalyticsSession = localyticsSession;
    }

    @Override
    public void openSession() {
        mLocalyticsSession.open();
    }

    @Override
    public void closeSession() {
        mLocalyticsSession.close();
        mLocalyticsSession.upload();
    }

    @Override
    public void flush() {
        mLocalyticsSession.upload();
    }

    @Override
    public void trackScreen(String screenTag) {
        mLocalyticsSession.tagScreen(screenTag);
    }

    @Override
    public void trackPlaybackEvent(PlaybackEventData eventData) {
        if (eventData.isStopEvent()) {
            Map<String, String> eventAttributes = new HashMap<String, String>();
            eventAttributes.put("context", eventData.getTrackSourceInfo().getOriginScreen());
            eventAttributes.put("track_id", String.valueOf(eventData.getTrack().getId()));

            final int duration = eventData.getTrack().duration;
            eventAttributes.put("track_length_ms", String.valueOf(duration));
            eventAttributes.put("track_length_bucket", getTrackLengthBucket(duration));

            eventAttributes.put("duration_ms", String.valueOf(eventData.getListenTime()));
            eventAttributes.put("percent_listened", getPercentListenedBucket(eventData, duration));

            // be careful of null values allowed in attributes, will propogate a hard to trace exception
            final String genreOrTag = eventData.getTrack().getGenreOrTag();
            if (ScTextUtils.isNotBlank(genreOrTag)) {
                eventAttributes.put("tag", genreOrTag);
            }

            if (eventData.getTrackSourceInfo().isFromPlaylist()) {
                eventAttributes.put("set_id", String.valueOf(eventData.getTrackSourceInfo().getPlaylistId()));
                eventAttributes.put("set_owner", eventData.isPlayingOwnPlaylist() ? "you" : "other");
            }
            eventAttributes.put("stop_reason", getStopReason(eventData));

            if (android.util.Log.isLoggable(TAG, android.util.Log.DEBUG)) {
                logAttributes(eventAttributes);
            }

            mLocalyticsSession.tagEvent(LocalyticsEvents.LISTEN, eventAttributes);
        }
    }

    private void logAttributes(Map<String, String> eventAttributes) {
        final Objects.ToStringHelper toStringHelper = Objects.toStringHelper("EventAttributes");
        for (String key : eventAttributes.keySet()){
            toStringHelper.add(key, eventAttributes.get(key));
        }
        Log.i(TAG, toStringHelper.toString());
    }

    private String getPercentListenedBucket(PlaybackEventData eventData, int duration) {
        double percentListened = ((double) eventData.getListenTime()) / duration;
        if (percentListened < .05) {
            return "<5%";
        } else if (percentListened <= .25) {
            return "5% to 25%";
        } else if (percentListened <= .75) {
            return "25% to 75%";
        } else {
            return ">75%";
        }
    }

    private String getTrackLengthBucket(int duration) {
        if (duration < 60 * 1000) {
            return "<1min";
        } else if (duration <= 10 * 60 * 1000) {
            return "1min to 10min";
        } else if (duration <= 30 * 60 * 1000) {
            return "10min to 30min";
        } else if (duration <= 60 * 60 * 1000) {
            return "30min to 1hr";
        } else {
            return ">1hr";
        }
    }

    private String getStopReason(PlaybackEventData eventData) {
        switch (eventData.getStopReason()) {
            case PlaybackEventData.STOP_REASON_PAUSE:
                return "pause";
            case PlaybackEventData.STOP_REASON_BUFFERING:
                return "buffering";
            case PlaybackEventData.STOP_REASON_SKIP:
                return "skip";
            case PlaybackEventData.STOP_REASON_TRACK_FINISHED:
                return "track_finished";
            case PlaybackEventData.STOP_REASON_END_OF_QUEUE:
                return "end_of_content";
            case PlaybackEventData.STOP_REASON_NEW_QUEUE:
                return "context_change";
            case PlaybackEventData.STOP_REASON_ERROR:
                return "playback_error";
            default:
                throw new IllegalArgumentException("Unexpected stop reason : " + eventData.getStopReason());
        }
    }
}
