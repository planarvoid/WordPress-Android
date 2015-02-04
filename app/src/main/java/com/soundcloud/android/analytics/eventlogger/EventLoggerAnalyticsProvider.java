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
import com.soundcloud.android.events.UserSessionEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;

import android.support.v4.util.ArrayMap;

import javax.inject.Inject;
import java.util.Map;

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
    public void handleOnboardingEvent(OnboardingEvent event) {
    }

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
        } else if (event instanceof ScreenEvent) {
            handleScreenEvent((ScreenEvent) event);
        }
    }

    private void handleScreenEvent(ScreenEvent event) {
        final String screenTag = event.get(ScreenEvent.KEY_SCREEN);
        Map<String, String> eventAttributes = new ArrayMap<>();
        eventAttributes.put("context", screenTag);
        trackEvent(event.getTimeStamp(), urlBuilder.build(event));
    }

    @Override
    public void handleUserSessionEvent(UserSessionEvent event) {}

    private void handleLeaveBehindTracking(AdOverlayTrackingEvent event) {
        final String url = urlBuilder.build(event);
        trackEvent(event.getTimeStamp(), url);
    }

    private void handleVisualAdImpression(VisualAdImpressionEvent event) {
        trackEvent(event.getTimeStamp(), urlBuilder.build(event));
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
            trackEvent(event.getTimeStamp(), urlBuilder.build(event));
        }
    }

    @Override
    public void handlePlaybackPerformanceEvent(final PlaybackPerformanceEvent eventData) {
        trackEvent(eventData.getTimeStamp(), urlBuilder.build(eventData));
    }

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {
        trackEvent(eventData.getTimestamp(), urlBuilder.build(eventData));
    }

    private void trackAudioAdImpression(PlaybackSessionEvent eventData) {
        trackEvent(eventData.getTimeStamp(), urlBuilder.buildForAudioAdImpression(eventData));
    }

    private void trackAudioAdFinished(PlaybackSessionEvent eventData) {
        trackEvent(eventData.getTimeStamp(), urlBuilder.buildForAdFinished(eventData));
    }

    private void trackAudioPlayEvent(PlaybackSessionEvent eventData) {
        trackEvent(eventData.getTimeStamp(), urlBuilder.buildForAudioEvent(eventData));
    }

    private void trackEvent(long timeStamp, String url) {
        eventTracker.trackEvent(new TrackingRecord(timeStamp, BACKEND_NAME, url));
    }

}
