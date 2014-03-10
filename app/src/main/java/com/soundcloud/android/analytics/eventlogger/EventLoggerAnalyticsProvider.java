package com.soundcloud.android.analytics.eventlogger;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;

import javax.inject.Inject;

public class EventLoggerAnalyticsProvider implements AnalyticsProvider {

    @Inject
    EventLogger mEventLogger;

    public EventLoggerAnalyticsProvider() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    protected EventLoggerAnalyticsProvider(EventLogger eventLogger) {
        mEventLogger = eventLogger;
    }

    @Override
    public void flush() {
        mEventLogger.flush();
    }

    @Override
    public void handleCurrentUserChangedEvent(CurrentUserChangedEvent event) {
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
    public void handleUIEvent(UIEvent event) {
    }

    @Override
    public void handleOnboardingEvent(OnboardingEvent event) {
    }

    @Override
    public void handleSearchEvent(SearchEvent searchEvent) {}

    @Override
    public void handlePlaybackEvent(PlaybackEvent eventData) {
        mEventLogger.trackEvent(eventData);
    }
}
