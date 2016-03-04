package com.soundcloud.android.analytics.promoted;

import com.soundcloud.android.analytics.DefaultAnalyticsProvider;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.AdPlaybackProgressEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;

import javax.inject.Inject;
import java.util.List;

// This class is all about multiplexing out tracking events
@SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
public class PromotedAnalyticsProvider extends DefaultAnalyticsProvider {

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
        } else if (event instanceof AdPlaybackProgressEvent) {
            handleAdPlaybackProgressEvent((AdPlaybackProgressEvent) event);
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

    private void handleAdPlaybackProgressEvent(AdPlaybackProgressEvent event) {
        trackAllUrls(event.getTimestamp(), event.getQuartileTrackingUrls());
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
