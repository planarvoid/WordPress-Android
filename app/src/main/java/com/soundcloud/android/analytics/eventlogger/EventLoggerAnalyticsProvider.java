package com.soundcloud.android.analytics.eventlogger;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.utils.Log;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;

@SuppressWarnings("PMD.UncommentedEmptyMethod")
public class EventLoggerAnalyticsProvider implements AnalyticsProvider {


    @Inject
    EventLogger eventLogger;
    @Inject
    EventLoggerParamsBuilder eventLoggerParamsBuilder;

    @Inject
    public EventLoggerAnalyticsProvider() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    protected EventLoggerAnalyticsProvider(EventLogger eventLogger, EventLoggerParamsBuilder eventLoggerParamsBuilder) {
        this.eventLogger = eventLogger;
        this.eventLoggerParamsBuilder = eventLoggerParamsBuilder;
    }

    @Override
    public void flush() {
        eventLogger.flush();
    }

    @Override
    public void handleCurrentUserChangedEvent(CurrentUserChangedEvent event) {
    }

    @Override
    public void handleActivityLifeCycleEvent(ActivityLifeCycleEvent event) {
    }

    @Override
    public void handlePlayerLifeCycleEvent(PlayerLifeCycleEvent event) {
        if (event.getKind() == PlayerLifeCycleEvent.STATE_DESTROYED) {
            eventLogger.stop();
        }
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
            final String params = eventLoggerParamsBuilder.buildFromPlaybackEvent(eventData);
            final EventLoggerEvent event = new EventLoggerEvent(eventData.getTimeStamp(), EventLoggerEventTypes.PLAYBACK.getPath(), params);
            eventLogger.trackEvent(event);

        } catch (UnsupportedEncodingException e) {
            Log.e(EventLogger.TAG, "Unable to process playback event ", e);
        }

    }

    @Override
    public void handlePlaybackPerformanceEvent(final PlaybackPerformanceEvent eventData) {
        final String params = eventLoggerParamsBuilder.buildFromPlaybackPerformanceEvent(eventData);
        final EventLoggerEvent event = new EventLoggerEvent(eventData.getTimeStamp(), EventLoggerEventTypes.PLAYBACK_PERFORMANCE.getPath(), params);
        eventLogger.trackEvent(event);
    }

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {
        final String params = eventLoggerParamsBuilder.buildFromPlaybackErrorEvent(eventData);
        final EventLoggerEvent event = new EventLoggerEvent(eventData.getTimestamp(), EventLoggerEventTypes.PLAYBACK_ERROR.getPath(), params);
        eventLogger.trackEvent(event);
    }

    @Override
    public void handlePlayControlEvent(PlayControlEvent eventData) {}

}
