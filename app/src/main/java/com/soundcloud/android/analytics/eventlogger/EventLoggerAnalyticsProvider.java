package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.analytics.DefaultAnalyticsProvider;
import com.soundcloud.android.analytics.EventTrackingManager;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.events.AdDeliveryEvent;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.AdPlaybackErrorEvent;
import com.soundcloud.android.events.AdPlaybackSessionEvent;
import com.soundcloud.android.events.AdRequestEvent;
import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.FacebookInvitesEvent;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.OfflinePerformanceEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.settings.SettingKey;
import dagger.Lazy;

import android.content.SharedPreferences;

import javax.inject.Inject;

public class EventLoggerAnalyticsProvider extends DefaultAnalyticsProvider {

    public static final String BATCH_BACKEND_NAME = "boogaloo";

    private final EventTrackingManager eventTrackingManager;
    private final Lazy<EventLoggerJsonDataBuilder> dataBuilderV0;
    private final Lazy<EventLoggerV1JsonDataBuilder> dataBuilderV1;
    private final SharedPreferences sharedPreferences;
    private final FeatureFlags featureFlags;

    @Inject
    public EventLoggerAnalyticsProvider(EventTrackingManager eventTrackingManager,
                                        Lazy<EventLoggerJsonDataBuilder> dataBuilderV0,
                                        Lazy<EventLoggerV1JsonDataBuilder> dataBuilderV1,
                                        SharedPreferences sharedPreferences,
                                        FeatureFlags featureFlags) {
        this.sharedPreferences = sharedPreferences;
        this.dataBuilderV0 = dataBuilderV0;
        this.dataBuilderV1 = dataBuilderV1;
        this.eventTrackingManager = eventTrackingManager;
        this.featureFlags = featureFlags;
    }

    @Override
    public void flush() {
        eventTrackingManager.flush(BATCH_BACKEND_NAME);
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
        } else if (event instanceof UpgradeFunnelEvent) {
            handleUpsellEvent((UpgradeFunnelEvent) event);
        } else if (event instanceof FacebookInvitesEvent) {
            handleFacebookInvitesEvent((FacebookInvitesEvent) event);
        } else if (event instanceof CollectionEvent) {
            handleCollectionEvent((CollectionEvent) event);
        } else if (event instanceof OfflineInteractionEvent) {
            handleOfflineInteractionEvent((OfflineInteractionEvent) event);
        } else if (event instanceof OfflinePerformanceEvent) {
            handleOfflinePerformanceEvent((OfflinePerformanceEvent) event);
        } else if (event instanceof AdRequestEvent) {
            handleAdRequestEvent((AdRequestEvent) event);
        } else if (event instanceof AdDeliveryEvent) {
            handleAdDeliveryEvent((AdDeliveryEvent) event);
        } else if (event instanceof AdPlaybackSessionEvent) {
            handleAdPlaybackSessionEvent((AdPlaybackSessionEvent) event);
        } else if (event instanceof AdPlaybackErrorEvent) {
            handleAdPlaybackErrorEvent((AdPlaybackErrorEvent) event);
        }
    }

    private void handleOfflineInteractionEvent(OfflineInteractionEvent event) {
        trackEvent(event.getTimestamp(), dataBuilderV1.get().buildForOfflineInteractionEvent(event));
    }

    private void handleOfflinePerformanceEvent(OfflinePerformanceEvent event) {
        trackEvent(event.getTimestamp(), dataBuilderV1.get().buildForOfflinePerformanceEvent(event));
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
        trackEvent(event.getTimestamp(), dataBuilderV1.get().buildForScreenEvent(event));
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

    private void handleUpsellEvent(UpgradeFunnelEvent event) {
        trackEvent(event.getTimestamp(), dataBuilderV1.get().buildForUpsell(event));
    }

    private void handlePlaybackSessionEvent(final PlaybackSessionEvent event) {
        trackAudioSessionEvent(event);
    }

    private void handleUIEvent(UIEvent event) {
        switch (event.getKind()) {
            case UIEvent.KIND_LIKE:
            case UIEvent.KIND_UNLIKE:
            case UIEvent.KIND_REPOST:
            case UIEvent.KIND_UNREPOST:
            case UIEvent.KIND_SHARE:
            case UIEvent.KIND_FOLLOW:
            case UIEvent.KIND_UNFOLLOW:
            case UIEvent.KIND_PLAYER_OPEN:
            case UIEvent.KIND_PLAYER_CLOSE:
            case UIEvent.KIND_PLAY_QUEUE_OPEN:
            case UIEvent.KIND_PLAY_QUEUE_CLOSE:
            case UIEvent.KIND_SWIPE_SKIP:
            case UIEvent.KIND_SYSTEM_SKIP:
            case UIEvent.KIND_BUTTON_SKIP:
            case UIEvent.KIND_NAVIGATION:
            case UIEvent.KIND_SHUFFLE:
            case UIEvent.KIND_VIDEO_AD_FULLSCREEN:
            case UIEvent.KIND_VIDEO_AD_SHRINK:
            case UIEvent.KIND_AD_CLICKTHROUGH:
            case UIEvent.KIND_SKIP_AD_CLICK:
            case UIEvent.KIND_PLAY_QUEUE_SHUFFLE:
            case UIEvent.KIND_PLAY_QUEUE_TRACK_REORDER:
            case UIEvent.KIND_PLAY_QUEUE_TRACK_REMOVE:
            case UIEvent.KIND_PLAY_QUEUE_TRACK_REMOVE_UNDO:
            case UIEvent.KIND_PLAY_QUEUE_REPEAT:
            case UIEvent.KIND_PLAY_NEXT:
            case UIEvent.KIND_RECOMMENDED_PLAYLISTS:
                trackEvent(event.getTimestamp(), dataBuilderV1.get().buildForUIEvent(event));
                break;
            default:
                // no-op, ignoring certain types
                break;
        }

        if (featureFlags.isEnabled(Flag.HOLISTIC_TRACKING) && dataBuilderV1.get().isInteractionEvent(event)) {
            trackEvent(event.getTimestamp(), dataBuilderV1.get().buildForInteractionEvent(event));
        }
    }

    private void handleSearchEvent(SearchEvent event) {
        switch (event.kind()) {
            case SEARCH_RESULTS:
            case SEARCH_SUBMIT:
            case SEARCH_SUGGESTION:
                trackEvent(event.getTimestamp(), dataBuilderV0.get().build(event));
                break;
            case SEARCH_FORMULATION_INIT:
            case SEARCH_FORMULATION_END:
            case SEARCH_LOCAL_SUGGESTION:
                trackEvent(event.getTimestamp(), dataBuilderV1.get().buildForSearchEvent(event));
            default:
                // no-op, ignoring certain types
                break;
        }
    }

    private void handleAdRequestEvent(AdRequestEvent eventData) {
        trackEvent(eventData.getTimestamp(), dataBuilderV1.get().buildForAdRequest(eventData));
    }

    private void handleAdDeliveryEvent(AdDeliveryEvent eventData) {
        trackEvent(eventData.getTimestamp(), dataBuilderV1.get().buildForAdDelivery(eventData));
    }

    private void handleAdPlaybackSessionEvent(AdPlaybackSessionEvent eventData) {
        switch (eventData.getKind()) {
            case AdPlaybackSessionEvent.EVENT_KIND_QUARTILE:
                trackAdProgressQuartile(eventData);
                break;
            case AdPlaybackSessionEvent.EVENT_KIND_PLAY:
            case AdPlaybackSessionEvent.EVENT_KIND_STOP:
            case AdPlaybackSessionEvent.EVENT_KIND_CHECKPOINT:
                if (eventData.shouldReportStart()) {
                    trackAdImpression(eventData);
                } else if (eventData.hasAdFinished()) {
                    trackAdFinished(eventData);
                }
                trackAdSessionEvent(eventData);
                break;
            default:
                break;
        }
    }

    @Override
    public void handlePlaybackPerformanceEvent(final PlaybackPerformanceEvent eventData) {
        final String data = eventData.isAd() ?
                            dataBuilderV1.get().buildForRichMediaPerformance(eventData) :
                            dataBuilderV0.get().build(eventData);
        trackEvent(eventData.getTimestamp(), data);
    }

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {
        trackEvent(eventData.getTimestamp(), dataBuilderV0.get().build(eventData));
    }

    private void trackAdImpression(AdPlaybackSessionEvent eventData) {
        trackEvent(eventData.getTimestamp(), dataBuilderV1.get().buildForAdImpression(eventData));
    }

    private void trackAdFinished(AdPlaybackSessionEvent eventData) {
        trackEvent(eventData.getTimestamp(), dataBuilderV1.get().buildForAdFinished(eventData));
    }

    private void trackAdSessionEvent(AdPlaybackSessionEvent eventData) {
        trackEvent(eventData.getTimestamp(), dataBuilderV1.get().buildForRichMediaSessionEvent(eventData));
    }

    private void trackAdProgressQuartile(AdPlaybackSessionEvent eventData) {
        trackEvent(eventData.getTimestamp(), dataBuilderV1.get().buildForAdProgressQuartileEvent(eventData));
    }

    private void handleAdPlaybackErrorEvent(AdPlaybackErrorEvent eventData) {
        trackEvent(eventData.getTimestamp(), dataBuilderV1.get().buildForRichMediaErrorEvent(eventData));
    }

    private void trackAudioSessionEvent(PlaybackSessionEvent eventData) {
        trackEvent(eventData.getTimestamp(), dataBuilderV1.get().buildForAudioEvent(eventData));
    }

    private void trackEvent(long timeStamp, String data) {
        eventTrackingManager.trackEvent(new TrackingRecord(timeStamp, BATCH_BACKEND_NAME, data));
        if (sharedPreferences.getBoolean(SettingKey.DEV_FLUSH_EVENTLOGGER_INSTANTLY, false)) {
            eventTrackingManager.flush(BATCH_BACKEND_NAME);
        }
    }

}
