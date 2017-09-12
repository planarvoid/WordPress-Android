package com.soundcloud.android.ads;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.rx.eventbus.EventBusV2;

import javax.inject.Inject;

public class VisualAdImpressionOperations {
    private final EventBusV2 eventBus;
    private final PlayQueueManager playQueueManager;
    private final AccountOperations accountOperations;

    private boolean impressionEventEmitted;

    @Inject
    VisualAdImpressionOperations(EventBusV2 eventBus,
                                 PlayQueueManager playQueueManager,
                                 AccountOperations accountOperations) {
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
        this.accountOperations = accountOperations;

    }

    void onUnlockCurrentImpression() {
        impressionEventEmitted = false;
    }

    void onState(State state) {
        if (!isAdVisible(state)) {
            return;
        }

        VisualAdImpressionEvent impressionEvent = createImpressionEvent(state);
        impressionEventEmitted = true;
        eventBus.publish(EventQueue.TRACKING, impressionEvent);
    }

    private boolean isAdVisible(State state) {
        return !impressionEventEmitted && state.playerIsExpanding && state.isAppInForeground;
    }

    private VisualAdImpressionEvent createImpressionEvent(State state) {
        return VisualAdImpressionEvent.create(
                (AudioAd) state.adData,
                accountOperations.getLoggedInUserUrn(),
                playQueueManager.getCurrentTrackSourceInfo()
        );
    }

    static final class State {
        private final AdData adData;
        private final boolean isAppInForeground;
        private final boolean playerIsExpanding;

        public State(AdData adData,
                     boolean isAppInForeground,
                     boolean playerIsExpanding) {
            this.adData = adData;
            this.isAppInForeground = isAppInForeground;
            this.playerIsExpanding = playerIsExpanding;
        }
    }
}
