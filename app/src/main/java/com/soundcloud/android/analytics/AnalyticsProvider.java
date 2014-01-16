package com.soundcloud.android.analytics;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.events.SocialEvent;

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
     * Signals to the analytics provider that a life-cycle event occurred in the playback service
     */
    void handlePlayerLifeCycleEvent(PlayerLifeCycleEvent event);

    /**
     * Signals to the analytics provider that a screen (Activity, Fragment or View) was being opened.
     *
     * @param screenTag the tag under which to track the screen
     */
    void handleScreenEvent(String screenTag);

    /**
     * Signals to the analytics provider that a playback event has occurred
     *
     * @param eventData what the playback event consisted of
     */
    void handlePlaybackEvent(PlaybackEvent eventData);

    /**
     * Signals to the analytics provider that a social event has occurred
     *
     * @param event social event information
     */
    void handleSocialEvent(SocialEvent event);

    /**
     * Signals to the analytics provider that a onboarding event has occurred
     *
     * @param event onboarding event information
     */
    void handleOnboardingEvent(OnboardingEvent event);
}
