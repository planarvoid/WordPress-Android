package com.soundcloud.android.analytics.promoted;

import com.soundcloud.android.analytics.DefaultAnalyticsProvider;
import com.soundcloud.android.analytics.EventTrackingManager;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.AdPlaybackSessionEvent;
import com.soundcloud.android.events.InlayAdImpressionEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PrestitialAdImpressionEvent;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;

import javax.inject.Inject;
import java.util.Collections;
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
        final List<String> trackingUrls;

        // TODO: Merge all impression events into single AdImpressionEvent
        if (event instanceof PlaybackSessionEvent) {
            trackingUrls = handlePlaybackSessionEvent((PlaybackSessionEvent) event);
        } else if (event instanceof UIEvent) {
            UIEvent uiEvent = (UIEvent) event;
            trackingUrls = uiEvent.adTrackingUrls().or(Collections.emptyList());
        } else if (event instanceof VisualAdImpressionEvent) {
            trackingUrls = ((VisualAdImpressionEvent) event).impressionUrls();
        } else if (event instanceof InlayAdImpressionEvent) {
            trackingUrls = ((InlayAdImpressionEvent) event).impressionUrls();
        } else if (event instanceof AdOverlayTrackingEvent) {
            trackingUrls = ((AdOverlayTrackingEvent) event).trackingUrls();
        } else if (event instanceof PromotedTrackingEvent) {
            trackingUrls = ((PromotedTrackingEvent) event).trackingUrls();
        } else if (event instanceof AdPlaybackSessionEvent) {
            AdPlaybackSessionEvent sessionEvent = (AdPlaybackSessionEvent) event;
            trackingUrls = sessionEvent.trackingUrls().or(Collections.emptyList());
        } else if (event instanceof PrestitialAdImpressionEvent) {
            trackingUrls = ((PrestitialAdImpressionEvent) event).impressionUrls();
        } else {
            trackingUrls = Collections.emptyList();
        }

        trackAllUrls(event.getTimestamp(), trackingUrls);
    }

    private List<String> handlePlaybackSessionEvent(PlaybackSessionEvent event) {
        if (event.isPromotedTrack() && event.isPlayAdShouldReportAdStart() && event.promotedPlayUrls().isPresent()) {
            return event.promotedPlayUrls().get();
        } else {
            return Collections.emptyList();
        }
    }

    private void trackAllUrls(long timeStamp, List<String> urls) {
        if (!urls.isEmpty()) {
            for (String url : urls) {
                eventTrackingManager.trackEvent(new TrackingRecord(timeStamp, BACKEND_NAME, url));
            }
            eventTrackingManager.flush(BACKEND_NAME);
        }
    }
}
