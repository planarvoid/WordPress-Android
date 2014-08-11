package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.TrackingEvent;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;

import javax.inject.Inject;

@SuppressWarnings("PMD.UncommentedEmptyMethod")
public class EventLoggerAnalyticsProvider implements AnalyticsProvider {

    public static final String BACKEND_NAME = "eventlogger";

    private final EventTracker eventTracker;
    private final EventLoggerUrlBuilder urlBuilder;

    @Inject
    public EventLoggerAnalyticsProvider(EventTracker eventTracker, EventLoggerUrlBuilder urlBuilder) {
        this.eventTracker = eventTracker;
        this.urlBuilder = urlBuilder;
    }

    @Override
    public void flush() {
        eventTracker.flush(BACKEND_NAME);
    }

    @Override
    public void handleCurrentUserChangedEvent(CurrentUserChangedEvent event) {
    }

    @Override
    public void handleActivityLifeCycleEvent(ActivityLifeCycleEvent event) {
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
    public void handlePlaybackSessionEvent(final PlaybackSessionEvent eventData) {
        if (eventData.isAd() && eventData.isAtStart()) {
            trackAdImpression(eventData);
        }
        trackAudioPlayEvent(eventData);
    }

    @Override
    public void handlePlaybackPerformanceEvent(final PlaybackPerformanceEvent eventData) {
        final String url = urlBuilder.buildForAudioPerformanceEvent(eventData);
        final TrackingEvent event = new TrackingEvent(eventData.getTimeStamp(), BACKEND_NAME, url);
        eventTracker.trackEvent(event);
    }

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {
        final String url = urlBuilder.buildForAudioErrorEvent(eventData);
        final TrackingEvent event = new TrackingEvent(eventData.getTimestamp(), BACKEND_NAME, url);
        eventTracker.trackEvent(event);
    }

    @Override
    public void handlePlayControlEvent(PlayControlEvent eventData) {}

    private void trackAdImpression(PlaybackSessionEvent eventData) {
        final String url = urlBuilder.buildForAdImpression(eventData);
        trackEvent(eventData.getTimeStamp(), url);
    }

    private void trackAudioPlayEvent(PlaybackSessionEvent eventData) {
        final String url = urlBuilder.buildForAudioEvent(eventData);
        trackEvent(eventData.getTimeStamp(), url);
    }

    private void trackEvent(long timeStamp, String url) {
        final TrackingEvent event = new TrackingEvent(timeStamp, BACKEND_NAME, url);
        eventTracker.trackEvent(event);
    }

}
