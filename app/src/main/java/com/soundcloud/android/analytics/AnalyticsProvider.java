package com.soundcloud.android.analytics;

import com.soundcloud.android.events.SocialEvent;
import com.soundcloud.android.events.PlaybackEventData;

/**
 * Implementations of this interface will be sending information to a specific analytics provider
 */
public interface AnalyticsProvider {
    /**
     * Signals to the analytics provider that user session is open/started
     * Calls to this method should be idempotent and should not signal to the analytics
     * provider of multiple sessions if one already is open or raise an error
     */
    void openSession();

    /**
     * Signals to the analytics provider that the user is session closed/stopped
     * Calls to this method should be idempotent and should not signal to the analytics
     * provider of multiple session closures if a session is not open or raise an error
     */
    void closeSession();

    /**
     * Signals to the analytics provider that pending event/session data should be transmitted
     * to the remote service.
     */
    void flush();

    /**
     * Signals to the analytics provider that a screen (Activity or Fragment) was being opened.
     *
     * @param screenTag the tag under which to track the screen
     */
    void trackScreen(String screenTag);

    /**
     * Signals to the analytics provider that a playback event has occurred
     *
     * @param eventData what the playback event consisted of
     */
    void trackPlaybackEvent(PlaybackEventData eventData);

    /**
     * Signals to the analytics provider that a social event has occurred
     *
     * @param event social event information
     */
    void trackSocialEvent(SocialEvent event);
}
