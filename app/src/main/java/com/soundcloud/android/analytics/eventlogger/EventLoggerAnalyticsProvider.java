package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UserSessionEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import javax.inject.Inject;

@SuppressWarnings("PMD.UncommentedEmptyMethod")
public class EventLoggerAnalyticsProvider implements AnalyticsProvider {

    public static final String LEGACY_BACKEND_NAME = "eventlogger";
    public static final String BATCH_BACKEND_NAME = "boogaloo";

    public static final String BATCH_ENDPOINT = "https://eventgateway.soundcloud.com/v1/events";

    private final EventTracker eventTracker;
    private final EventLoggerDataBuilder dataBuilder;
    private final FeatureFlags flags;

    private final String currentBackend;

    @Inject
    public EventLoggerAnalyticsProvider(EventTracker eventTracker, EventLoggerDataBuilderFactory dataBuilderFactory, FeatureFlags flags) {
        currentBackend = flags.isEnabled(Flag.EVENTLOGGER_BATCHING) ? BATCH_BACKEND_NAME : LEGACY_BACKEND_NAME;
        dataBuilder = dataBuilderFactory.create(currentBackend);
        this.eventTracker = eventTracker;
        this.flags = flags;
    }

    @Override
    public void flush() {
        eventTracker.flush(currentBackend);
    }

    @Override
    public void handleCurrentUserChangedEvent(CurrentUserChangedEvent event) {}

    @Override
    public void handleActivityLifeCycleEvent(ActivityLifeCycleEvent event) {}

    @Override
    public void handleOnboardingEvent(OnboardingEvent event) {}

    @Override
    public void handleTrackingEvent(TrackingEvent event) {
        if (event instanceof PlaybackSessionEvent) {
            handlePlaybackSessionEvent((PlaybackSessionEvent) event);
        } else if (event instanceof UIEvent) {
            handleUIEvent((UIEvent) event);
        } else if (event instanceof VisualAdImpressionEvent) {
            handleVisualAdImpression((VisualAdImpressionEvent) event);
        } else if (event instanceof AdOverlayTrackingEvent) {
            handleLeaveBehindTracking((AdOverlayTrackingEvent) event);
        } else if (event instanceof ScreenEvent && flags.isEnabled(Flag.EVENTLOGGER_PAGE_VIEW_EVENTS)) {
            handleScreenEvent((ScreenEvent) event);
        }
    }

    private void handleScreenEvent(ScreenEvent event) {
        trackEvent(event.getTimeStamp(), dataBuilder.build(event));
    }

    @Override
    public void handleUserSessionEvent(UserSessionEvent event) {}

    private void handleLeaveBehindTracking(AdOverlayTrackingEvent event) {
        final String url = dataBuilder.build(event);
        trackEvent(event.getTimeStamp(), url);
    }

    private void handleVisualAdImpression(VisualAdImpressionEvent event) {
        if (AdOverlayTrackingEvent.KIND_CLICK.equals(event.getKind()) || AdOverlayTrackingEvent.KIND_IMPRESSION.equals(event.getKind())) {
            trackEvent(event.getTimeStamp(), dataBuilder.build(event));
        }
    }

    private void handlePlaybackSessionEvent(final PlaybackSessionEvent event) {
        if (event.isAd()) {
            if (event.isFirstPlay()) {
                trackAudioAdImpression(event);
            } else if (event.hasTrackFinished()) {
                trackAudioAdFinished(event);
            }
        }
        trackAudioPlayEvent(event);
    }

    private void handleUIEvent(UIEvent event) {
        if (UIEvent.KIND_AUDIO_AD_CLICK.equals(event.getKind()) || UIEvent.KIND_SKIP_AUDIO_AD_CLICK.equals(event.getKind())) {
            trackEvent(event.getTimeStamp(), dataBuilder.build(event));
        }
    }

    @Override
    public void handlePlaybackPerformanceEvent(final PlaybackPerformanceEvent eventData) {
        trackEvent(eventData.getTimeStamp(), dataBuilder.build(eventData));
    }

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {
        trackEvent(eventData.getTimestamp(), dataBuilder.build(eventData));
    }

    private void trackAudioAdImpression(PlaybackSessionEvent eventData) {
        trackEvent(eventData.getTimeStamp(), dataBuilder.buildForAudioAdImpression(eventData));
    }

    private void trackAudioAdFinished(PlaybackSessionEvent eventData) {
        trackEvent(eventData.getTimeStamp(), dataBuilder.buildForAdFinished(eventData));
    }

    private void trackAudioPlayEvent(PlaybackSessionEvent eventData) {
        trackEvent(eventData.getTimeStamp(), dataBuilder.buildForAudioEvent(eventData));
    }

    private void trackEvent(long timeStamp, String data) {
        eventTracker.trackEvent(new TrackingRecord(timeStamp, currentBackend, data));
    }

}
