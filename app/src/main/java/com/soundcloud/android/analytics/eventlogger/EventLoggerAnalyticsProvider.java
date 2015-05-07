package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UserSessionEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.settings.SettingKey;

import android.content.SharedPreferences;

import javax.inject.Inject;

@SuppressWarnings("PMD.UncommentedEmptyMethod")
public class EventLoggerAnalyticsProvider implements AnalyticsProvider {

    public static final String BATCH_BACKEND_NAME = "boogaloo";

    private final EventTracker eventTracker;
    private final EventLoggerJsonDataBuilder dataBuilder;
    private final FeatureFlags flags;
    private final SharedPreferences sharedPreferences;

    @Inject
    public EventLoggerAnalyticsProvider(EventTracker eventTracker, EventLoggerJsonDataBuilder dataBuilder, FeatureFlags flags, SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
        this.dataBuilder = dataBuilder;
        this.eventTracker = eventTracker;
        this.flags = flags;
    }

    @Override
    public void flush() {
        eventTracker.flush(BATCH_BACKEND_NAME);
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
        } else if (event instanceof SearchEvent) {
            handleSearchEvent((SearchEvent) event);
        } else if (event instanceof ForegroundEvent) {
            handleForegroundEvent((ForegroundEvent) event);
        }
    }

    private void handleForegroundEvent(ForegroundEvent event) {
        trackEvent(event.getTimeStamp(), dataBuilder.build(event));
    }

    private void handleScreenEvent(ScreenEvent event) {
        trackEvent(event.getTimeStamp(), dataBuilder.build(event));
    }

    @Override
    public void handleUserSessionEvent(UserSessionEvent event) {
    }

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

    private void handleSearchEvent(SearchEvent event) {
        if (flags.isEnabled(Flag.EVENTLOGGER_SEARCH_EVENTS)) {
            switch (event.getKind()) {
                case SearchEvent.KIND_RESULTS:
                case SearchEvent.KIND_SUBMIT:
                case SearchEvent.KIND_SUGGESTION:
                    trackEvent(event.getTimeStamp(), dataBuilder.build(event));
                    break;
                default:
                    // no-op, ignoring certain types
                    break;
            }
        }
    }

    @Override
    public void handlePlaybackPerformanceEvent(final PlaybackPerformanceEvent eventData) {
        if (eventData.getMetric() != PlaybackPerformanceEvent.METRIC_FRAGMENT_DOWNLOAD_RATE) {
            trackEvent(eventData.getTimeStamp(), dataBuilder.build(eventData));
        }
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
        eventTracker.trackEvent(new TrackingRecord(timeStamp, BATCH_BACKEND_NAME, data));
        if (sharedPreferences.getBoolean(SettingKey.DEV_FLUSH_EVENTLOGGER_INSTANTLY, false)) {
            eventTracker.flush(BATCH_BACKEND_NAME);
        }
    }
}
