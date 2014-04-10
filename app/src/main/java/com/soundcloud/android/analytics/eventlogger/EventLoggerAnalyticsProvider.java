package com.soundcloud.android.analytics.eventlogger;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.utils.Log;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;

public class EventLoggerAnalyticsProvider implements AnalyticsProvider {


    @Inject
    EventLogger mEventLogger;
    @Inject
    EventLoggerParamsBuilder mEventLoggerParamsBuilder;

    @Inject
    public EventLoggerAnalyticsProvider() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    protected EventLoggerAnalyticsProvider(EventLogger eventLogger, EventLoggerParamsBuilder eventLoggerParamsBuilder) {
        mEventLogger = eventLogger;
        mEventLoggerParamsBuilder = eventLoggerParamsBuilder;
    }

    @Override
    public void flush() {
        mEventLogger.flush();
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
            mEventLogger.stop();
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
    public void handlePlaybackEvent(final PlaybackEvent eventData) {
        try {
            final String params = mEventLoggerParamsBuilder.buildFromPlaybackEvent(eventData);
            final EventLoggerEvent event = new EventLoggerEvent(eventData.getTimeStamp(), EventLoggerEventTypes.PLAYBACK.getPath(), params);
            mEventLogger.trackEvent(event);

        } catch (UnsupportedEncodingException e) {
            Log.e(EventLogger.TAG, "Unable to process playback event ", e);
        }

    }

    @Override
    public void handlePlaybackPerformanceEvent(final PlaybackPerformanceEvent eventData) {
        final String params = mEventLoggerParamsBuilder.buildFromPlaybackPerformanceEvent(eventData);
        final EventLoggerEvent event = new EventLoggerEvent(eventData.getTimeStamp(), EventLoggerEventTypes.PLAYBACK_PERFORMANCE.getPath(), params);
        mEventLogger.trackEvent(event);
    }

    @Override
    public void handlePlayControlEvent(PlayControlEvent eventData) {}

}
