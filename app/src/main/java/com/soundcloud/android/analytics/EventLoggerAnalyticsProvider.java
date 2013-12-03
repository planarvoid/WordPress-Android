package com.soundcloud.android.analytics;

import com.soundcloud.android.dagger.DaggerDependencyInjector;
import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.tracking.eventlogger.EventLoggerModule;
import com.soundcloud.android.tracking.eventlogger.PlayEventTracker;

import javax.inject.Inject;

public class EventLoggerAnalyticsProvider implements AnalyticsProvider {

    @Inject
    PlayEventTracker mPlayEventTracker;

    public EventLoggerAnalyticsProvider() {
        new DaggerDependencyInjector().fromAppGraphWithModules(new EventLoggerModule()).inject(this);
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
