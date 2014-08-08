package com.soundcloud.android.analytics.playcounts;

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
public class PlayCountAnalyticsProvider implements AnalyticsProvider {

    public static final String BACKEND_NAME = "play_counts";

    private final EventTracker eventTracker;
    private final PlayCountUrlBuilder urlBuilder;

    @Inject
    public PlayCountAnalyticsProvider(EventTracker eventTracker, PlayCountUrlBuilder urlBuilder) {
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
    public void handlePlaybackSessionEvent(PlaybackSessionEvent eventData) {
        // only track the first play
        if (eventData.getProgress() == 0) {
            final String url = urlBuilder.buildUrl(eventData);
            final TrackingEvent event = new TrackingEvent(eventData.getTimeStamp(), BACKEND_NAME, url);
            eventTracker.trackEvent(event);
            eventTracker.flush(BACKEND_NAME);
        }
    }

    @Override
    public void handlePlaybackPerformanceEvent(PlaybackPerformanceEvent eventData) {

    }

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {

    }

    @Override
    public void handlePlayControlEvent(PlayControlEvent eventData) {

    }

    @Override
    public void handleUIEvent(UIEvent event) {

    }

    @Override
    public void handleOnboardingEvent(OnboardingEvent event) {

    }

    @Override
    public void handleSearchEvent(SearchEvent searchEvent) {

    }
}
