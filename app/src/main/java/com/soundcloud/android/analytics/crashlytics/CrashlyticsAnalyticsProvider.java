package com.soundcloud.android.analytics.crashlytics;

import com.crashlytics.android.Crashlytics;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UserSessionEvent;

import javax.inject.Inject;

public class CrashlyticsAnalyticsProvider implements AnalyticsProvider {

    @Inject
    CrashlyticsAnalyticsProvider() {}

    @Override
    public void flush() {}

    @Override
    public void handleCurrentUserChangedEvent(CurrentUserChangedEvent event) {}

    @Override
    public void handleActivityLifeCycleEvent(ActivityLifeCycleEvent event) {}

    @Override
    public void handlePlaybackPerformanceEvent(PlaybackPerformanceEvent eventData) {}

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {}

    @Override
    public void handleOnboardingEvent(OnboardingEvent event) {}

    @Override
    public void handleTrackingEvent(TrackingEvent event) {
        if (event instanceof ScreenEvent) {
            handleScreenEvent(event);
        }
    }

    @Override
    public void handleUserSessionEvent(UserSessionEvent event) {}

    private void handleScreenEvent(TrackingEvent event) {
        Crashlytics.setString("Screen", event.get(ScreenEvent.KEY_SCREEN));
    }
}
