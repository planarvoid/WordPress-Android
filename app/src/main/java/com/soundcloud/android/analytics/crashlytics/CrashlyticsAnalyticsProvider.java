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
import com.soundcloud.android.properties.ApplicationProperties;
import io.fabric.sdk.android.Fabric;

import android.util.Log;

import javax.inject.Inject;

public class CrashlyticsAnalyticsProvider implements AnalyticsProvider {

    private static final String TAG = "CrashlyticsLogger";

    private final boolean debugBuild;

    @Inject
    CrashlyticsAnalyticsProvider(ApplicationProperties applicationProperties) {
        debugBuild = applicationProperties.isDebugBuild();
    }

    @Override
    public void flush() {
    }

    @Override
    public void handleCurrentUserChangedEvent(CurrentUserChangedEvent event) {
    }

    @Override
    public void handleActivityLifeCycleEvent(ActivityLifeCycleEvent event) {
    }

    @Override
    public void handlePlaybackPerformanceEvent(PlaybackPerformanceEvent eventData) {
    }

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {
    }

    @Override
    public void handleOnboardingEvent(OnboardingEvent event) {
    }

    @Override
    public void handleTrackingEvent(TrackingEvent event) {
        if (Fabric.isInitialized() && shouldLogEvent(event)) {
            final String message = event.toString();
            if (debugBuild) {
                Crashlytics.log(Log.DEBUG, TAG, message);
            } else {
                Crashlytics.log(message);
            }
        }
    }

    @Override
    public void handleUserSessionEvent(UserSessionEvent event) {
    }

    private boolean shouldLogEvent(TrackingEvent event) {
        return event instanceof ScreenEvent || event instanceof UIEvent;
    }
}
