package com.soundcloud.android.ads;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.AdOverlayEvent;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.rx.eventbus.EventBusV2;

import javax.inject.Inject;

public class AdOverlayImpressionOperations {
    private final EventBusV2 eventBus;
    private final AccountOperations accountOperations;

    private boolean impressionEventEmitted;

    @Inject
    AdOverlayImpressionOperations(EventBusV2 eventBus, AccountOperations accountOperations) {
        this.eventBus = eventBus;
        this.accountOperations = accountOperations;
    }

    public void onUnlockCurrentImpression(AdOverlayEvent event) {
        if (event.getKind() == AdOverlayEvent.HIDDEN) {
            impressionEventEmitted = false;
        }
    }

    public void onVisualImpressionState(VisualImpressionState state) {
        if (isAdOverlayVisible(state)) {
            AdOverlayTrackingEvent trackingEvent = createTrackingEvent(state);
            lockCurrentImpression();
            eventBus.publish(EventQueue.TRACKING, trackingEvent);
        }
    }

    private boolean isAdOverlayVisible(VisualImpressionState visualImpressionState) {
        return !impressionEventEmitted && visualImpressionState.adOverlayIsVisible && visualImpressionState.playerIsExpanding && visualImpressionState.isAppInForeground;
    }

    private void lockCurrentImpression() {
        impressionEventEmitted = true;
    }

    private AdOverlayTrackingEvent createTrackingEvent(VisualImpressionState visualImpressionState) {
        return AdOverlayTrackingEvent.forImpression(
                visualImpressionState.adData,
                visualImpressionState.currentPlayingUrn,
                accountOperations.getLoggedInUserUrn(),
                visualImpressionState.trackSourceInfo);
    }

    public static final class VisualImpressionState {
        private final boolean adOverlayIsVisible;
        private final boolean isAppInForeground;
        private final boolean playerIsExpanding;
        private final Urn currentPlayingUrn;
        private final VisualAdData adData;
        private final TrackSourceInfo trackSourceInfo;

        public VisualImpressionState(boolean adOverlayIsVisible, boolean isAppInForeground, boolean playerIsExpanding,
                                     Urn currentPlayingUrn, VisualAdData adData, TrackSourceInfo trackSourceInfo) {
            this.isAppInForeground = isAppInForeground;
            this.adOverlayIsVisible = adOverlayIsVisible;
            this.playerIsExpanding = playerIsExpanding;
            this.currentPlayingUrn = currentPlayingUrn;
            this.adData = adData;
            this.trackSourceInfo = trackSourceInfo;
        }
    }
}
