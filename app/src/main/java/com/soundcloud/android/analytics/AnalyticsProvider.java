package com.soundcloud.android.analytics;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UserSessionEvent;

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

    /**
     * Signals to the analytics provider that the currently logged-in user has changed
     * @param event the new user that has logged in
     */
    void handleCurrentUserChangedEvent(CurrentUserChangedEvent event);

    /**
     * Signals to the analytics provider that a life-cycle event occurred in an Activity (created, paused, etc.)
     */
    void handleActivityLifeCycleEvent(ActivityLifeCycleEvent event);

    /**
     * Signals to the analytics provider that a playback performance event has occurred
     *
     * @param eventData what the playback performance event consisted of
     */
    void handlePlaybackPerformanceEvent(PlaybackPerformanceEvent eventData);

    /**
     * Signals to the analytics provider that a playback error event has occurred
     *
     * @param eventData what the playback error event consisted of
     */
    void handlePlaybackErrorEvent(PlaybackErrorEvent eventData);

    /**
     * Signals to the analytics provider that a onboarding event has occurred
     *
     * @param event onboarding event information
     */
    void handleOnboardingEvent(OnboardingEvent event);


    void handleTrackingEvent(TrackingEvent event);

    void handleUserSessionEvent(UserSessionEvent event);

    void onAppCreated(Context context);
}
