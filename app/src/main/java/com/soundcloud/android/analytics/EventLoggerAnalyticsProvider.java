package com.soundcloud.android.analytics;

import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.tracking.eventlogger.PlayEventTracker;

import android.content.Context;

public class EventLoggerAnalyticsProvider implements AnalyticsProvider {

    PlayEventTracker mPlayEventTracker;

    public EventLoggerAnalyticsProvider(Context context) {
        this(new PlayEventTracker(context));
    }

    public EventLoggerAnalyticsProvider(PlayEventTracker playEventTracker) {
        mPlayEventTracker = playEventTracker;
    }

    @Override
    public void openSession() {
    }

    @Override
    public void closeSession() {
    }

    @Override
    public void flush() {
    }

    @Override
    public void trackScreen(String screenTag) {
    }

    @Override
    public void trackPlaybackEvent(PlaybackEventData eventData) {
        mPlayEventTracker.trackEvent(eventData);
    }
}
