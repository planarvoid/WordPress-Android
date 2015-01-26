package com.soundcloud.android.analytics.adjust;

import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.UserSessionEvent;
import com.soundcloud.android.events.TrackingEvent;

import javax.inject.Inject;

public class AdjustAnalyticsProvider implements AnalyticsProvider {
    private final AdjustWrapper adjustWrapper;

    @Inject
    public AdjustAnalyticsProvider(AdjustWrapper adjustWrapper) {
        this.adjustWrapper = adjustWrapper;
    }

    @Override
    public void handleActivityLifeCycleEvent(ActivityLifeCycleEvent event) {
        if (event.getKind() == ActivityLifeCycleEvent.ON_RESUME_EVENT) {
            adjustWrapper.onResume();
        } else if (event.getKind() == ActivityLifeCycleEvent.ON_PAUSE_EVENT) {
            adjustWrapper.onPause();
        }
    }
    
    @Override
    public void flush() {
        // Not implemented
    }

    @Override
    public void handleCurrentUserChangedEvent(CurrentUserChangedEvent event) {
        // Not implemented
    }

    @Override
    public void handlePlaybackPerformanceEvent(PlaybackPerformanceEvent eventData) {
        // Not implemented
    }

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {
        // Not implemented
    }

    @Override
    public void handleOnboardingEvent(OnboardingEvent event) {
        // Not implemented
    }

    @Override
    public void handleTrackingEvent(TrackingEvent event) {
        // Not implemented
    }

    @Override
    public void handleUserSessionEvent(UserSessionEvent event) {
        // Not implemented
    }
}
