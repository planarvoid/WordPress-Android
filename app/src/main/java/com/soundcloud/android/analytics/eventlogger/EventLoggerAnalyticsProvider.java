package com.soundcloud.android.analytics.eventlogger;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.SocialEvent;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.dagger.DaggerDependencyInjector;
import com.soundcloud.android.dagger.DependencyInjector;

import javax.inject.Inject;

public class EventLoggerAnalyticsProvider implements AnalyticsProvider {

    @Inject
    EventLogger mEventLogger;

    public EventLoggerAnalyticsProvider() {
        this(new DaggerDependencyInjector());
    }

    @VisibleForTesting
    protected EventLoggerAnalyticsProvider(DependencyInjector dependencyInjector) {
        dependencyInjector.fromAppGraphWithModules(new EventLoggerModule()).inject(this);
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
    public void trackSocialEvent(SocialEvent event) {
    }

    @Override
    public void trackPlaybackEvent(PlaybackEvent eventData) {
        mEventLogger.trackEvent(eventData);
    }
}
