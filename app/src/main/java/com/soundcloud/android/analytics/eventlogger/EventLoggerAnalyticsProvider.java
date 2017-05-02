package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.analytics.DefaultAnalyticsProvider;
import com.soundcloud.android.analytics.EventTrackingManager;
import com.soundcloud.android.analytics.TrackingRecord;
import com.soundcloud.android.events.AdDeliveryEvent;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.AdPlaybackErrorEvent;
import com.soundcloud.android.events.AdPlaybackSessionEvent;
import com.soundcloud.android.events.AdRequestEvent;
import com.soundcloud.android.events.AdRichMediaSessionEvent;
import com.soundcloud.android.events.CollectionEvent;
import com.soundcloud.android.events.FacebookInvitesEvent;
import com.soundcloud.android.events.ForegroundEvent;
import com.soundcloud.android.events.InlayAdImpressionEvent;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.OfflinePerformanceEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.PrestitialAdImpressionEvent;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.ScrollDepthEvent;
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
    private final Lazy<EventLoggerV1JsonDataBuilder> dataBuilderV1;
    private final SharedPreferences sharedPreferences;
    private final FeatureFlags featureFlags;

    @Inject
    public EventLoggerAnalyticsProvider(EventTrackingManager eventTrackingManager,
                                        Lazy<EventLoggerV1JsonDataBuilder> dataBuilderV1,
                                        SharedPreferences sharedPreferences,
                                        FeatureFlags featureFlags) {
        this.sharedPreferences = sharedPreferences;
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
        } else if (event instanceof InlayAdImpressionEvent) {
            handleStreamAdImpression((InlayAdImpressionEvent) event);
        } else if (event instanceof AdOverlayTrackingEvent) {
            handleLeaveBehindTracking((AdOverlayTrackingEvent) event);
        } else if (event instanceof PrestitialAdImpressionEvent) {
            handlePrestitialAdImpression((PrestitialAdImpressionEvent) event);
        } else if (event instanceof ScreenEvent) {
            handleScreenEvent((ScreenEvent) event);
        } else if (event instanceof SearchEvent) {
            trackEvent(event.getTimestamp(), dataBuilderV1.get().buildForSearchEvent((SearchEvent) event));
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
        } else if (event instanceof AdRichMediaSessionEvent) {
            handleAdRichMediaSessionEvent((AdRichMediaSessionEvent) event);
        } else if (event instanceof AdPlaybackErrorEvent) {
            handleAdPlaybackErrorEvent((AdPlaybackErrorEvent) event);
        } else if (event instanceof ScrollDepthEvent) {
            handleScrollDepthEvent((ScrollDepthEvent) event);
        }
    }

    private void handleScrollDepthEvent(ScrollDepthEvent event) {
        trackEvent(event.getTimestamp(), dataBuilderV1.get().buildForScrollDepthEvent(event));
    }

    private void handleOfflineInteractionEvent(OfflineInteractionEvent event) {
        if (event.sendToEventLogger()) {
            trackEvent(event.getTimestamp(), dataBuilderV1.get().buildForOfflineInteractionEvent(event));
        }
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
        trackEvent(event.getTimestamp(), dataBuilderV1.get().buildForForgroundEvent(event));
    }

    private void handleScreenEvent(ScreenEvent event) {
        trackEvent(event.getTimestamp(), dataBuilderV1.get().buildForScreenEvent(event));
    }

    private void handleLeaveBehindTracking(AdOverlayTrackingEvent event) {
        final String url = dataBuilderV1.get().buildForAdOverlayTracking(event);
        trackEvent(event.getTimestamp(), url);
    }

    private void handleVisualAdImpression(VisualAdImpressionEvent event) {
        trackEvent(event.getTimestamp(), dataBuilderV1.get().buildForVisualAdImpression(event));
    }

    private void handlePrestitialAdImpression(PrestitialAdImpressionEvent event) {
        trackEvent(event.getTimestamp(), dataBuilderV1.get().buildForPrestitialAd(event));
    }

    private void handleStreamAdImpression(InlayAdImpressionEvent event) {
        trackEvent(event.getTimestamp(), dataBuilderV1.get().buildForStreamAd(event));
    }

    private void handlePromotedEvent(PromotedTrackingEvent eventData) {
        trackEvent(eventData.getTimestamp(), dataBuilderV1.get().buildForPromotedTracking(eventData));
    }

    private void handleUpsellEvent(UpgradeFunnelEvent event) {
        trackEvent(event.getTimestamp(), dataBuilderV1.get().buildForUpsell(event));
    }

    private void handlePlaybackSessionEvent(final PlaybackSessionEvent event) {
        trackAudioSessionEvent(event);
    }

    private void handleUIEvent(UIEvent event) {
        switch (event.kind()) {
            case LIKE:
            case UNLIKE:
            case REPOST:
            case UNREPOST:
            case SHARE:
            case FOLLOW:
            case UNFOLLOW:
            case PLAYER_OPEN:
            case PLAYER_CLOSE:
            case PLAY_QUEUE_OPEN:
            case PLAY_QUEUE_CLOSE:
            case SWIPE_SKIP:
            case SYSTEM_SKIP:
            case BUTTON_SKIP:
            case NAVIGATION:
            case SHUFFLE:
            case VIDEO_AD_FULLSCREEN:
            case VIDEO_AD_SHRINK:
            case VIDEO_AD_MUTE:
            case VIDEO_AD_UNMUTE:
            case AD_CLICKTHROUGH:
            case SKIP_AD_CLICK:
            case PLAY_QUEUE_SHUFFLE:
            case PLAY_QUEUE_TRACK_REORDER:
            case PLAY_QUEUE_TRACK_REMOVE:
            case PLAY_QUEUE_TRACK_REMOVE_UNDO:
            case PLAY_QUEUE_REPEAT:
            case PLAY_NEXT:
            case RECOMMENDED_PLAYLISTS:
            case MORE_PLAYLISTS_BY_USER:
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

    private void handleAdRequestEvent(AdRequestEvent eventData) {
        trackEvent(eventData.getTimestamp(), dataBuilderV1.get().buildForAdRequest(eventData));
    }

    private void handleAdDeliveryEvent(AdDeliveryEvent eventData) {
        trackEvent(eventData.getTimestamp(), dataBuilderV1.get().buildForAdDelivery(eventData));
    }

    private void handleAdPlaybackSessionEvent(AdPlaybackSessionEvent eventData) {
        switch (eventData.eventKind()) {
            case START:
            case FINISH:
            case QUARTILE:
                trackEvent(eventData.getTimestamp(), dataBuilderV1.get().buildForAdPlaybackSessionEvent(eventData));
                break;
            default:
                // no-op, ignoring certain types
                break;
        }
    }

    @Override
    public void handlePlaybackPerformanceEvent(final PlaybackPerformanceEvent eventData) {
        final String data = eventData.isAd() ?
                            dataBuilderV1.get().buildForRichMediaPerformance(eventData) :
                            dataBuilderV1.get().buildForPlaybackPerformance(eventData);
        trackEvent(eventData.timestamp(), data);
    }

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {
        trackEvent(eventData.getTimestamp(), dataBuilderV1.get().buildForPlaybackError(eventData));
    }

    @Override
    public void handleAdRichMediaSessionEvent(AdRichMediaSessionEvent eventData) {
        trackEvent(eventData.getTimestamp(), dataBuilderV1.get().buildForRichMediaSessionEvent(eventData));
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
