package com.soundcloud.android.analytics.playcounts;

import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UserSessionEvent;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import android.content.Context;

import javax.inject.Inject;

@SuppressWarnings("PMD.UncommentedEmptyMethod")
public class PlayCountAnalyticsProvider implements AnalyticsProvider {

    public static final String BACKEND_NAME = "play_counts";

    private final EventTracker eventTracker;
    private final PlayCountUrlBuilder urlBuilder;
    private final FeatureFlags featureFlags;

    @Inject
    public PlayCountAnalyticsProvider(EventTracker eventTracker, PlayCountUrlBuilder urlBuilder, FeatureFlags featureFlags) {
        this.eventTracker = eventTracker;
        this.urlBuilder = urlBuilder;
        this.featureFlags = featureFlags;
    }

    @Override
    public void flush() {
        eventTracker.flush(BACKEND_NAME);
    }

    @Override
    public void handleCurrentUserChangedEvent(CurrentUserChangedEvent event) {

    }

    @Override
    public void onAppCreated(Context context) {
        /* no op */
    }

    @Override
    public void handleActivityLifeCycleEvent(ActivityLifeCycleEvent event) {

    }

    @Override
    public void handlePlaybackPerformanceEvent(PlaybackPerformanceEvent eventData) {

    }

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {

    }

    @Override
    public void handleOnboardingEvent(OnboardingEvent event) {

    }

    @Override
    public void handleTrackingEvent(TrackingEvent event) {
        if (event instanceof PlaybackSessionEvent && featureFlags.isDisabled(Flag.EVENTLOGGER_AUDIO_V1)) {
            handlePlaybackSessionEvent((PlaybackSessionEvent) event);
        }
    }

    @Override
    public void handleUserSessionEvent(UserSessionEvent event) {}

    private void handlePlaybackSessionEvent(PlaybackSessionEvent eventData) {
        // only track the first play
        if (eventData.isFirstPlay()) {
            final String url = urlBuilder.buildUrl(eventData);
            final TrackingRecord event = new TrackingRecord(eventData.getTimestamp(), BACKEND_NAME, url);
            eventTracker.trackEvent(event);
            eventTracker.flush(BACKEND_NAME);
        }
    }
}
