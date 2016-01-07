package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.FacebookInvitesEvent;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.settings.SettingKey;
import dagger.Lazy;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;

@SuppressWarnings("PMD.UncommentedEmptyMethod")
public class EventLoggerAnalyticsProvider implements AnalyticsProvider {

    public static final String BATCH_BACKEND_NAME = "boogaloo";

    private final EventTracker eventTracker;
    private final Lazy<EventLoggerJsonDataBuilder> dataBuilderV0;
    private final Lazy<EventLoggerV1JsonDataBuilder> dataBuilderV1;
    private final SharedPreferences sharedPreferences;
    private final FeatureFlags featureFlags;

    @Inject
    public EventLoggerAnalyticsProvider(EventTracker eventTracker,
                                        Lazy<EventLoggerJsonDataBuilder> dataBuilderV0,
                                        Lazy<EventLoggerV1JsonDataBuilder> dataBuilderV1,
                                        SharedPreferences sharedPreferences, FeatureFlags featureFlags) {
        this.sharedPreferences = sharedPreferences;
        this.dataBuilderV0 = dataBuilderV0;
        this.dataBuilderV1 = dataBuilderV1;
        this.eventTracker = eventTracker;
        this.featureFlags = featureFlags;
    }

    @Override
    public void flush() {
        eventTracker.flush(BATCH_BACKEND_NAME);
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
        } else if (event instanceof PromotedTrackingEvent) {
            handlePromotedEvent((PromotedTrackingEvent) event);
        } else if (event instanceof UpgradeTrackingEvent) {
            handleUpsellEvent((UpgradeTrackingEvent) event);
        } else if (event instanceof FacebookInvitesEvent) {
            handleFacebookInvitesEvent((FacebookInvitesEvent) event);
        } else if (event instanceof CollectionEvent) {
            handleCollectionEvent((CollectionEvent) event);
        }
    }

    private void handleCollectionEvent(CollectionEvent event) {
        trackEvent(event.getTimestamp(), dataBuilderV1.get().buildForCollectionEvent(event));
    }

    private void handleFacebookInvitesEvent(FacebookInvitesEvent event) {
        trackEvent(event.getTimestamp(), dataBuilderV1.get().buildForFacebookInvites(event));
    }

    private void handleForegroundEvent(ForegroundEvent event) {
        trackEvent(event.getTimestamp(), dataBuilderV0.get().build(event));
    }

    private void handleScreenEvent(ScreenEvent event) {
        trackEvent(event.getTimestamp(), dataBuilderV0.get().build(event));
    }

    private void handleLeaveBehindTracking(AdOverlayTrackingEvent event) {
        final String url = dataBuilderV0.get().build(event);
        trackEvent(event.getTimestamp(), url);
    }

    private void handleVisualAdImpression(VisualAdImpressionEvent event) {
        trackEvent(event.getTimestamp(), dataBuilderV0.get().build(event));
    }

    private void handlePromotedEvent(PromotedTrackingEvent eventData) {
        trackEvent(eventData.getTimestamp(), dataBuilderV0.get().build(eventData));
    }

    private void handleUpsellEvent(UpgradeTrackingEvent event) {
        trackEvent(event.getTimestamp(), dataBuilderV0.get().build(event));
    }

    private void handlePlaybackSessionEvent(final PlaybackSessionEvent event) {
        if (event.isAd()) {
            if (event.isFirstPlay()) {
                trackAudioAdImpression(event);
            } else if (event.hasTrackFinished()) {
                trackAudioAdFinished(event);
            }
        }
        trackAudioSessionEvent(event);
    }

    private void handleUIEvent(UIEvent event) {
        switch (event.getKind()) {
            case UIEvent.KIND_AUDIO_AD_CLICK:
            case UIEvent.KIND_SKIP_AUDIO_AD_CLICK:
                trackEvent(event.getTimestamp(), dataBuilderV0.get().build(event));
                break;
            case UIEvent.KIND_LIKE:
            case UIEvent.KIND_UNLIKE:
            case UIEvent.KIND_REPOST:
            case UIEvent.KIND_UNREPOST:
                trackEvent(event.getTimestamp(), dataBuilderV1.get().buildForUIEvent(event));
                break;
            case UIEvent.KIND_OFFLINE_LIKES_ADD:
            case UIEvent.KIND_OFFLINE_LIKES_REMOVE:
            case UIEvent.KIND_OFFLINE_COLLECTION_ADD:
            case UIEvent.KIND_OFFLINE_COLLECTION_REMOVE:
            case UIEvent.KIND_OFFLINE_PLAYLIST_ADD:
            case UIEvent.KIND_OFFLINE_PLAYLIST_REMOVE:
            case UIEvent.KIND_SHARE:
                trackEvent(event.getTimestamp(), dataBuilderV1.get().buildForUIEvent(event));
                break;
            default:
                // no-op, ignoring certain types
                break;
        }
    }

    private void handleSearchEvent(SearchEvent event) {
        switch (event.getKind()) {
            case SearchEvent.KIND_RESULTS:
            case SearchEvent.KIND_SUBMIT:
            case SearchEvent.KIND_SUGGESTION:
                trackEvent(event.getTimestamp(), dataBuilderV0.get().build(event));
                break;
            default:
                // no-op, ignoring certain types
                break;
        }
    }

    @Override
    public void handlePlaybackPerformanceEvent(final PlaybackPerformanceEvent eventData) {
        trackEvent(eventData.getTimestamp(), dataBuilderV0.get().build(eventData));
    }

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {
        trackEvent(eventData.getTimestamp(), dataBuilderV0.get().build(eventData));
    }

    private void trackAudioAdImpression(PlaybackSessionEvent eventData) {
        trackEvent(eventData.getTimestamp(), dataBuilderV0.get().buildForAudioAdImpression(eventData));
    }

    private void trackAudioAdFinished(PlaybackSessionEvent eventData) {
        trackEvent(eventData.getTimestamp(), dataBuilderV0.get().buildForAdFinished(eventData));
    }

    private void trackAudioSessionEvent(PlaybackSessionEvent eventData) {
        if (featureFlags.isEnabled(Flag.EVENTLOGGER_AUDIO_V1)) {
            trackEvent(eventData.getTimestamp(), dataBuilderV1.get().buildForAudioEvent(eventData));
        } else {
            trackEvent(eventData.getTimestamp(), dataBuilderV0.get().buildForAudioEvent(eventData));
        }
    }

    private void trackEvent(long timeStamp, String data) {
        eventTracker.trackEvent(new TrackingRecord(timeStamp, BATCH_BACKEND_NAME, data));
        if (sharedPreferences.getBoolean(SettingKey.DEV_FLUSH_EVENTLOGGER_INSTANTLY, false)) {
            eventTracker.flush(BATCH_BACKEND_NAME);
        }
    }

}
