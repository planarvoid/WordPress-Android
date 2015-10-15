package com.soundcloud.android.analytics;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.TrackingEvent;

import android.content.Context;

/**
 * Implementations of this interface will be sending information to a specific analytics provider
 */
public interface AnalyticsProvider {
    /**
     * Signals to the analytics provider that pending event/session data should be transmitted
     * to the remote service.
     */
    void flush();

    void handleCurrentUserChangedEvent(CurrentUserChangedEvent event);

    void handleActivityLifeCycleEvent(ActivityLifeCycleEvent event);

    void handlePlaybackPerformanceEvent(PlaybackPerformanceEvent eventData);

    void handlePlaybackErrorEvent(PlaybackErrorEvent eventData);

    void handleOnboardingEvent(OnboardingEvent event);

    void handleTrackingEvent(TrackingEvent event);

    void onAppCreated(Context context);
}
