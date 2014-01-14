package com.soundcloud.android.analytics.eventlogger;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
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
    public void flush() {
        mEventLogger.flush();
    }

    @Override
    public void handleActivityLifeCycleEvent(ActivityLifeCycleEvent event) {
    }

    @Override
    public void handlePlayerLifeCycleEvent(PlayerLifeCycleEvent event) {
        if (event.getKind() == PlayerLifeCycleEvent.STATE_DESTROYED) {
            mEventLogger.stop();
        }
    }

    @Override
    public void handleScreenEvent(String screenTag) {
    }

    @Override
    public void handleSocialEvent(SocialEvent event) {
    }

    @Override
    public void handlePlaybackEvent(PlaybackEvent eventData) {
        mEventLogger.trackEvent(eventData);
    }
}
