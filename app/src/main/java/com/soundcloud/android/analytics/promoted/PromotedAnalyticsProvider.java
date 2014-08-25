package com.soundcloud.android.analytics.promoted;

import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.TrackingEvent;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AudioAdCompanionImpressionEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;

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
    public void handlePlaybackSessionEvent(PlaybackSessionEvent event) {
        if (event.isAd()) {
            trackAdPlayback(event);
        }
    }

    private void trackAdPlayback(PlaybackSessionEvent event) {
        if (event.isFirstPlay()) {
            trackAllUrls(event.getTimeStamp(), event.getAudioAdImpressionUrls());
        } else if (event.hasTrackFinished()) {
            trackAllUrls(event.getTimeStamp(), event.getAudioAdFinishUrls());
        }
    }

    @Override
    public void handlePlaybackPerformanceEvent(PlaybackPerformanceEvent eventData) {

    }

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {

    }

    @Override
    public void handlePlayControlEvent(PlayControlEvent eventData) {

    }

    @Override
    public void handleUIEvent(UIEvent event) {

        List<String> urls;
        switch (event.getKind()) {
            case AUDIO_AD_CLICK:
                urls = event.getAudioAdClickthroughUrls();
                break;
            case SKIP_AUDIO_AD_CLICK:
                urls = event.getAudioAdSkipUrls();
                break;
            default:
                return;
        }

        trackAllUrls(event.getTimestamp(), urls);
    }

    @Override
    public void handleOnboardingEvent(OnboardingEvent event) {

    }

    @Override
    public void handleSearchEvent(SearchEvent event) {

    }

    @Override
    public void handleAudioAdCompanionImpression(AudioAdCompanionImpressionEvent event) {
        trackAllUrls(event.getTimeStamp(), event.getImpressionUrls());
    }

    private void trackAllUrls(long timeStamp, List<String> urls) {
        for (String url : urls) {
            eventTracker.trackEvent(new TrackingEvent(timeStamp, BACKEND_NAME, url));
        }
    }
}
