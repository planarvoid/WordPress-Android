package com.soundcloud.android.analytics.promoted;

import com.soundcloud.android.analytics.DefaultAnalyticsProvider;
import com.soundcloud.android.analytics.EventTrackingManager;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.AdPlaybackSessionEvent;
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
    private final EventTrackingManager eventTrackingManager;

    @Inject
    public PromotedAnalyticsProvider(EventTrackingManager eventTrackingManager) {
        this.eventTrackingManager = eventTrackingManager;
    }

    @Override
    public void flush() {
        eventTrackingManager.flush(BACKEND_NAME);
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
        } else if (event instanceof AdPlaybackSessionEvent) {
            handleAdPlaybackSessionEvent((AdPlaybackSessionEvent) event);
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

    private void handleAdPlaybackSessionEvent(AdPlaybackSessionEvent event) {
        trackAllUrls(event.getTimestamp(), event.getTrackingUrls());
    }

    private void handlePlaybackSessionEvent(PlaybackSessionEvent event) {
        if (event.isAd()) {
            if (event.shouldReportAdStart()) {
                trackAllUrls(event.getTimestamp(), event.getAudioAdImpressionUrls());
            } else if (event.hasTrackFinished()) {
                trackAllUrls(event.getTimestamp(), event.getAudioAdFinishUrls());
            }
        } else if (event.isPromotedTrack() && event.shouldReportAdStart()) {
            trackAllUrls(event.getTimestamp(), event.getPromotedPlayUrls());
        }
    }

    private void handleUIEvent(UIEvent event) {
        List<String> urls;
        switch (event.getKind()) {
            case UIEvent.KIND_AUDIO_AD_CLICK:
            case UIEvent.KIND_VIDEO_AD_CLICKTHROUGH:
                urls = event.getAdClickthroughUrls();
                break;
            case UIEvent.KIND_SKIP_AUDIO_AD_CLICK:
            case UIEvent.KIND_SKIP_VIDEO_AD_CLICK:
                urls = event.getAdSkipUrls();
                break;
            case UIEvent.KIND_VIDEO_AD_FULLSCREEN:
            case UIEvent.KIND_VIDEO_AD_SHRINK:
                urls = event.getVideoSizeChangeUrls();
                break;
            default:
                return;
        }

        trackAllUrls(event.getTimestamp(), urls);
    }

    private void trackAllUrls(long timeStamp, List<String> urls) {
        for (String url : urls) {
            eventTrackingManager.trackEvent(new TrackingRecord(timeStamp, BACKEND_NAME, url));
        }
        eventTrackingManager.flush(BACKEND_NAME);
    }
}
