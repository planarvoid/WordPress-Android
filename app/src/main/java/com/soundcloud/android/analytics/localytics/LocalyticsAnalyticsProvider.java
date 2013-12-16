package com.soundcloud.android.analytics.localytics;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.analytics.AnalyticsProperties;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.events.PlaybackEventData;

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
            eventAttributes.put("duration", String.valueOf(eventData.getListenTime()));
            eventAttributes.put("track_id", String.valueOf(eventData.getTrack().getId()));
            eventAttributes.put("track_length_ms", String.valueOf(eventData.getTrack().duration));
            eventAttributes.put("tag", eventData.getTrack().getGenreOrTag());
            mLocalyticsSession.tagEvent(LocalyticsEvents.LISTEN, eventAttributes);
        }
    }

}
