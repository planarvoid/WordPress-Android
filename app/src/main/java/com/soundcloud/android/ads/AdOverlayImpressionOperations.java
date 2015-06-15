package com.soundcloud.android.ads;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AdOverlayEvent;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.rx.eventbus.EventBus;
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
    private final PlayQueueManager playQueueManager;
    private final AccountOperations accountOperations;

    private final Func1<State, TrackingEvent> toTrackingEvent = new Func1<State, TrackingEvent>() {
        @Override
        public AdOverlayTrackingEvent call(State state) {
            return AdOverlayTrackingEvent.forImpression(
                    playQueueManager.getCurrentMetaData(),
                    playQueueManager.getCurrentTrackUrn(),
                    accountOperations.getLoggedInUserUrn(),
                    playQueueManager.getCurrentTrackSourceInfo());
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

    private final Func1<State, Boolean> isAdOverlayVisible = new Func1<State, Boolean>() {
        @Override
        public Boolean call(State state) {
            return !impressionEventEmitted && state.adOverlayIsVisible && state.playerIsExpanding && state.isAppInForeground;
        }
    };

    private final Func3<AdOverlayEvent, ActivityLifeCycleEvent, PlayerUIEvent, State> combineFunction =
            new Func3<AdOverlayEvent, ActivityLifeCycleEvent, PlayerUIEvent, State>() {
                @Override
                public State call(AdOverlayEvent leaveBehindEvent,
                                  ActivityLifeCycleEvent event,
                                  PlayerUIEvent playerUIEvent) {
                    return new State(
                            leaveBehindEvent.getKind() == AdOverlayEvent.SHOWN,
                            event.getKind() == ActivityLifeCycleEvent.ON_RESUME_EVENT,
                            playerUIEvent.getKind() == PlayerUIEvent.PLAYER_EXPANDED);
                }
            };

    private boolean impressionEventEmitted = false;

    @Inject
    AdOverlayImpressionOperations(EventBus eventBus, PlayQueueManager playQueueManager, AccountOperations accountOperations) {
        this.playQueueManager = playQueueManager;
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

    private static final class State {
        private final boolean adOverlayIsVisible;
        private final boolean isAppInForeground;
        private final boolean playerIsExpanding;

        public State(boolean adOverlayIsVisible, boolean isAppInForeground, boolean playerIsExpanding) {
            this.isAppInForeground = isAppInForeground;
            this.adOverlayIsVisible = adOverlayIsVisible;
            this.playerIsExpanding = playerIsExpanding;
        }
    }
}
