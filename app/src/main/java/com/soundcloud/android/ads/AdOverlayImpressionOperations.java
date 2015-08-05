package com.soundcloud.android.ads;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AdOverlayEvent;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func3;
import rx.subjects.Subject;

import javax.inject.Inject;

class AdOverlayImpressionOperations {
    private final Subject<ActivityLifeCycleEvent, ActivityLifeCycleEvent> activityLifeCycleQueue;
    private final Subject<PlayerUIEvent, PlayerUIEvent> playerUIEventQueue;
    private final Subject<AdOverlayEvent, AdOverlayEvent> adOverlayEventQueue;
    private final AccountOperations accountOperations;

    private final Func1<VisualImpressionState, TrackingEvent> toTrackingEvent = new Func1<VisualImpressionState, TrackingEvent>() {
        @Override
        public AdOverlayTrackingEvent call(VisualImpressionState visualImpressionState) {
            return AdOverlayTrackingEvent.forImpression(
                    visualImpressionState.adMetaData,
                    visualImpressionState.currentPlayingUrn,
                    accountOperations.getLoggedInUserUrn(),
                    visualImpressionState.trackSourceInfo);
        }
    };

    private final Action1<TrackingEvent> lockCurrentImpression = new Action1<TrackingEvent>() {
        @Override
        public void call(TrackingEvent event) {
            impressionEventEmitted = true;
        }
    };

    private final Action1<AdOverlayEvent> unlockCurrentImpression = new Action1<AdOverlayEvent>() {
        @Override
        public void call(AdOverlayEvent event) {
            if (event.getKind() == AdOverlayEvent.HIDDEN) {
                impressionEventEmitted = false;
            }
        }
    };

    private final Func1<VisualImpressionState, Boolean> isAdOverlayVisible = new Func1<VisualImpressionState, Boolean>() {
        @Override
        public Boolean call(VisualImpressionState visualImpressionState) {
            return !impressionEventEmitted && visualImpressionState.adOverlayIsVisible && visualImpressionState.playerIsExpanding && visualImpressionState.isAppInForeground;
        }
    };

    private final Func3<AdOverlayEvent, ActivityLifeCycleEvent, PlayerUIEvent, VisualImpressionState> combineFunction =
            new Func3<AdOverlayEvent, ActivityLifeCycleEvent, PlayerUIEvent, VisualImpressionState>() {
                @Override
                public VisualImpressionState call(AdOverlayEvent adOverlayEvent,
                                  ActivityLifeCycleEvent event,
                                  PlayerUIEvent playerUIEvent) {

                    return new VisualImpressionState(
                            adOverlayEvent.getKind() == AdOverlayEvent.SHOWN,
                            event.getKind() == ActivityLifeCycleEvent.ON_RESUME_EVENT,
                            playerUIEvent.getKind() == PlayerUIEvent.PLAYER_EXPANDED,
                            adOverlayEvent.getCurrentPlayingUrn(),
                            adOverlayEvent.getAdMetaData(), adOverlayEvent.getTrackSourceInfo());
                }
            };

    private boolean impressionEventEmitted = false;

    @Inject
    AdOverlayImpressionOperations(EventBus eventBus, AccountOperations accountOperations) {
        this.accountOperations = accountOperations;
        this.activityLifeCycleQueue = eventBus.queue(EventQueue.ACTIVITY_LIFE_CYCLE);
        this.playerUIEventQueue = eventBus.queue(EventQueue.PLAYER_UI);
        this.adOverlayEventQueue = eventBus.queue(EventQueue.AD_OVERLAY);
    }

    public Observable<TrackingEvent> trackImpression() {
        return Observable
                .combineLatest(
                        adOverlayEventQueue.doOnNext(unlockCurrentImpression),
                        activityLifeCycleQueue,
                        playerUIEventQueue,
                        combineFunction)
                .filter(isAdOverlayVisible)
                .map(toTrackingEvent)
                .doOnNext(lockCurrentImpression);
    }

    private static final class VisualImpressionState {
        private final boolean adOverlayIsVisible;
        private final boolean isAppInForeground;
        private final boolean playerIsExpanding;
        private final Urn currentPlayingUrn;
        private final PropertySet adMetaData;
        private final TrackSourceInfo trackSourceInfo;

        public VisualImpressionState(boolean adOverlayIsVisible, boolean isAppInForeground, boolean playerIsExpanding,
                                     Urn currentPlayingUrn, PropertySet adMetaData, TrackSourceInfo trackSourceInfo) {
            this.isAppInForeground = isAppInForeground;
            this.adOverlayIsVisible = adOverlayIsVisible;
            this.playerIsExpanding = playerIsExpanding;
            this.currentPlayingUrn = currentPlayingUrn;
            this.adMetaData = adMetaData;
            this.trackSourceInfo = trackSourceInfo;
        }
    }
}
