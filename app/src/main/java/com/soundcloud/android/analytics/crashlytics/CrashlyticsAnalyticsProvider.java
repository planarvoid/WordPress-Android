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
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UserSessionEvent;
import io.fabric.sdk.android.Fabric;

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
        if (Fabric.isInitialized()) {
            if (event instanceof ScreenEvent) {
                handleScreenEvent((ScreenEvent) event);
            } else if (event instanceof UIEvent) {
                handleUiEvent(((UIEvent) event));
            }
        }
    }

    @Override
    public void handleUserSessionEvent(UserSessionEvent event) {}

    private void handleUiEvent(UIEvent event) {
        Crashlytics.log(event.toString());
    }

    private void handleScreenEvent(ScreenEvent event) {
        Crashlytics.log(event.getScreenTag());
    }
}
