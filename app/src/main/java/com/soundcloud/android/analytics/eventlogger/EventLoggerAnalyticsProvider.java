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
import com.soundcloud.android.utils.Log;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;

@SuppressWarnings("PMD.UncommentedEmptyMethod")
public class EventLoggerAnalyticsProvider implements AnalyticsProvider {

    public static final String BACKEND_NAME = "eventlogger";
    private static final String TAG = "EventLogger";

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
        try {
            // if this is an ad
            //    use the ad url builder


            // do this normal code...
            final String url = urlBuilder.buildFromPlaybackEvent(eventData);



            final TrackingEvent event = new TrackingEvent(eventData.getTimeStamp(), BACKEND_NAME, url);
            eventTracker.trackEvent(event);

        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unable to process playback event ", e);
        }
    }

    @Override
    public void handlePlaybackPerformanceEvent(final PlaybackPerformanceEvent eventData) {
        final String url = urlBuilder.buildFromPlaybackPerformanceEvent(eventData);
        final TrackingEvent event = new TrackingEvent(eventData.getTimeStamp(), BACKEND_NAME, url);
        eventTracker.trackEvent(event);
    }

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {
        final String url = urlBuilder.buildFromPlaybackErrorEvent(eventData);
        final TrackingEvent event = new TrackingEvent(eventData.getTimestamp(), BACKEND_NAME, url);
        eventTracker.trackEvent(event);
    }

    @Override
    public void handlePlayControlEvent(PlayControlEvent eventData) {}

}
