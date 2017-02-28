package com.soundcloud.android.analytics.adjust;

import com.soundcloud.android.analytics.DefaultAnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.PurchaseEvent;
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
        adjustWrapper.trackPurchase(event.adjustToken(), event.price(), event.currency());
    }

    private void handleUpgradeTrackingEvent(UpgradeFunnelEvent event) {
        if (event.adjustToken().isPresent()) {
            adjustWrapper.trackEvent(event.adjustToken().get().toString());
        }
    }
}
