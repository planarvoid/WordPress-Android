package com.soundcloud.android.analytics.adjust;

import com.soundcloud.android.analytics.DefaultAnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.PurchaseEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;

import android.content.Context;

import javax.inject.Inject;

public class AdjustAnalyticsProvider extends DefaultAnalyticsProvider {

    private final AdjustWrapper adjustWrapper;

    @Inject
    AdjustAnalyticsProvider(AdjustWrapper adjustWrapper) {
        this.adjustWrapper = adjustWrapper;
    }

    @Override
    public void onAppCreated(Context context) {
        adjustWrapper.onCreate(context);
    }

    @Override
    public void handleActivityLifeCycleEvent(ActivityLifeCycleEvent event) {
        if (event.getKind() == ActivityLifeCycleEvent.ON_RESUME_EVENT) {
            adjustWrapper.onResume();
        } else if (event.getKind() == ActivityLifeCycleEvent.ON_PAUSE_EVENT) {
            adjustWrapper.onPause();
        }
    }

    @Override
    public void handleTrackingEvent(TrackingEvent event) {
        if (event instanceof PurchaseEvent) {
            handlePurchaseTracking((PurchaseEvent) event);
        } else if (event instanceof UpgradeFunnelEvent) {
            handleUpgradeTrackingEvent((UpgradeFunnelEvent) event);
        }
    }

    private void handlePurchaseTracking(PurchaseEvent event) {
        if (event.getKind().equals(PurchaseEvent.KIND_HIGH_TIER_SUB)) {
            adjustWrapper.trackPurchase(AdjustEventToken.HIGH_TIER_PURCHASE, event.getPrice(), event.getCurrency());
        }
    }

    private void handleUpgradeTrackingEvent(UpgradeFunnelEvent event) {
        if (event.isImpression()) {
            trackImpression(event);
        }
    }

    private void trackImpression(UpgradeFunnelEvent event) {
        switch (event.get(UpgradeFunnelEvent.KEY_ID)) {
            case UpgradeFunnelEvent.ID_WHY_ADS:
                adjustWrapper.trackEvent(AdjustEventToken.WHY_ADS);
                break;
            case UpgradeFunnelEvent.ID_UPGRADE_BUTTON:
                adjustWrapper.trackEvent(AdjustEventToken.CONVERSION);
                break;
            case UpgradeFunnelEvent.ID_UPGRADE_PROMO:
                adjustWrapper.trackEvent(AdjustEventToken.PROMO);
                break;
            case UpgradeFunnelEvent.ID_PLAYER:
                adjustWrapper.trackEvent(AdjustEventToken.HIGH_TIER_TRACK_PLAYED);
                break;
            case UpgradeFunnelEvent.ID_SEARCH_RESULTS_GO:
                adjustWrapper.trackEvent(AdjustEventToken.HIGH_TIER_SEARCH_RESULTS);
                break;
            case UpgradeFunnelEvent.ID_STREAM:
                adjustWrapper.trackEvent(AdjustEventToken.STREAM_UPSELL);
                break;
            case UpgradeFunnelEvent.ID_SETTINGS_UPGRADE:
                adjustWrapper.trackEvent(AdjustEventToken.OFFLINE_SETTINGS);
                break;
            case UpgradeFunnelEvent.ID_PLAYLIST_TRACKS:
                adjustWrapper.trackEvent(AdjustEventToken.PLAYLIST_TRACKS_UPSELL);
                break;
            case UpgradeFunnelEvent.ID_RESUBSCRIBE_BUTTON:
                adjustWrapper.trackEvent(AdjustEventToken.PLAN_DOWNGRADED);
                break;
            default:
                // Not all funnel events go to Adjust
                break;
        }
    }

}
