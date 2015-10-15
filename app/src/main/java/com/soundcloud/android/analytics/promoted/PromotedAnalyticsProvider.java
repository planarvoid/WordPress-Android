package com.soundcloud.android.analytics.promoted;

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
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;

import android.content.Context;

import javax.inject.Inject;
import java.util.List;

// This class is all about multiplexing out tracking events
@SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.UncommentedEmptyMethod"})
public class PromotedAnalyticsProvider implements AnalyticsProvider {

    public static final String BACKEND_NAME = "promoted";
    private final EventTracker eventTracker;

    @Inject
    public PromotedAnalyticsProvider(EventTracker eventTracker) {
        this.eventTracker = eventTracker;
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
        if (event instanceof PlaybackSessionEvent) {
            handlePlaybackSessionEvent((PlaybackSessionEvent) event);
        } else if (event instanceof UIEvent) {
            handleUIEvent((UIEvent) event);
        } else if (event instanceof VisualAdImpressionEvent) {
            handleVisualAdImpression((VisualAdImpressionEvent) event);
        } else if (event instanceof AdOverlayTrackingEvent) {
            handleLeaveBehindImpression((AdOverlayTrackingEvent) event);
        } else if (event instanceof PromotedTrackingEvent) {
            handlePromotedTrackEvent((PromotedTrackingEvent) event);
        }
    }

    private void handleLeaveBehindImpression(AdOverlayTrackingEvent event) {
        trackAllUrls(event.getTimestamp(), event.getTrackingUrls());
    }

    private void handleVisualAdImpression(VisualAdImpressionEvent event) {
        trackAllUrls(event.getTimestamp(), event.getImpressionUrls());
    }

    private void handlePromotedTrackEvent(PromotedTrackingEvent event) {
        trackAllUrls(event.getTimestamp(), event.getTrackingUrls());
    }

    private void handlePlaybackSessionEvent(PlaybackSessionEvent event) {
        if (event.isAd()) {
            if (event.isFirstPlay()) {
                trackAllUrls(event.getTimestamp(), event.getAudioAdImpressionUrls());
            } else if (event.hasTrackFinished()) {
                trackAllUrls(event.getTimestamp(), event.getAudioAdFinishUrls());
            }
        } else if (event.isPromotedTrack() && event.isFirstPlay()) {
            trackAllUrls(event.getTimestamp(), event.getPromotedPlayUrls());
        }
    }

    private void handleUIEvent(UIEvent event) {
        List<String> urls;
        switch (event.getKind()) {
            case UIEvent.KIND_AUDIO_AD_CLICK:
                urls = event.getAudioAdClickthroughUrls();
                break;
            case UIEvent.KIND_SKIP_AUDIO_AD_CLICK:
                urls = event.getAudioAdSkipUrls();
                break;
            default:
                return;
        }

        trackAllUrls(event.getTimestamp(), urls);
    }

    private void trackAllUrls(long timeStamp, List<String> urls) {
        for (String url : urls) {
            eventTracker.trackEvent(new TrackingRecord(timeStamp, BACKEND_NAME, url));
        }
        eventTracker.flush(BACKEND_NAME);
    }
}
