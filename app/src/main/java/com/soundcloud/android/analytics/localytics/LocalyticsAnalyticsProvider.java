package com.soundcloud.android.analytics.localytics;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.analytics.AnalyticsProperties;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

public class LocalyticsAnalyticsProvider implements AnalyticsProvider {
    private LocalyticsSession mLocalyticsSession;
    public LocalyticsAnalyticsProvider(Context context){
        this(new LocalyticsSession(context.getApplicationContext(),
                new AnalyticsProperties(context.getResources()).getLocalyticsAppKey()));
    }

    protected LocalyticsAnalyticsProvider(LocalyticsSession localyticsSession){
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
            eventAttributes.put("duration_ms", String.valueOf(eventData.getListenTime()));
            eventAttributes.put("track_id", String.valueOf(eventData.getTrack().getId()));
            eventAttributes.put("track_length_ms", String.valueOf(eventData.getTrack().duration));

            double percentListened = ((double) eventData.getListenTime()) / eventData.getTrack().duration;
            if (percentListened < .05){
                eventAttributes.put("percent_listened", "<5%");
            } else if (percentListened <= .25) {
                eventAttributes.put("percent_listened", "5% to 25%");
            } else if (percentListened <= .75) {
                eventAttributes.put("percent_listened", "25% to 75%");
            } else {
                eventAttributes.put("percent_listened", ">75%");
            }

            // be careful of null values allowed in attributes, will propogate a hard to trace exception
            final String genreOrTag = eventData.getTrack().getGenreOrTag();
            if (ScTextUtils.isNotBlank(genreOrTag)){
                eventAttributes.put("tag", genreOrTag);
            }

            if (eventData.getTrackSourceInfo().isFromPlaylist()){
                eventAttributes.put("set_id", String.valueOf(eventData.getTrackSourceInfo().getPlaylistId()));
                eventAttributes.put("set_owner", eventData.isPlayingOwnPlaylist() ? "you" : "other");
            }

            mLocalyticsSession.tagEvent(LocalyticsEvents.LISTEN, eventAttributes);
        }
    }

}
