package com.soundcloud.android.analytics;

/**
 * Implementations of this interface will be sending information to a specific analytics provider
 */
interface AnalyticsProvider {
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

}
