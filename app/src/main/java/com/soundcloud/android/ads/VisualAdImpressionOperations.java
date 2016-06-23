package com.soundcloud.android.ads;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueFunctions;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func3;
import rx.subjects.Subject;

import javax.inject.Inject;

public class VisualAdImpressionOperations {
    private final PlayQueueManager playQueueManager;
    private final AccountOperations accountOperations;
    private final AdsOperations adsOperations;
    private final Subject<ActivityLifeCycleEvent, ActivityLifeCycleEvent> activityLifeCycleQueue;
    private final Observable<CurrentPlayQueueItemEvent> currentTrackQueue;
    private final Subject<PlayerUIEvent, PlayerUIEvent> playerUIEventQueue;

    private final Func1<State, TrackingEvent> toTrackingEvent = new Func1<State, TrackingEvent>() {
        @Override
        public VisualAdImpressionEvent call(State state) {
            final PlayerAdData adData = (PlayerAdData) playQueueManager.getCurrentPlayQueueItem().getAdData().get();
            return new VisualAdImpressionEvent(
                    (AudioAd) adData,
                    state.currentTrackUrn,
                    accountOperations.getLoggedInUserUrn(),
                    playQueueManager.getCurrentTrackSourceInfo()
            );
        }
    };

    private final Action1<TrackingEvent> lockCurrentImpression = new Action1<TrackingEvent>() {
        @Override
        public void call(TrackingEvent event) {
            impressionEventEmitted = true;
        }
    };

    private final Action1<CurrentPlayQueueItemEvent> unlockCurrentImpression = new Action1<CurrentPlayQueueItemEvent>() {
        @Override
        public void call(CurrentPlayQueueItemEvent currentItemEvent) {
            impressionEventEmitted = false;
        }
    };

    private final Func1<State, Boolean> isAdVisible = new Func1<State, Boolean>() {
        @Override
        public Boolean call(State state) {
            return !impressionEventEmitted && state.currentTrackIsAnAudioAd && state.playerIsExpanding && state.isAppInForeground;
        }
    };

    private final Func3<ActivityLifeCycleEvent, CurrentPlayQueueItemEvent, PlayerUIEvent, State> combineFunction =
            new Func3<ActivityLifeCycleEvent, CurrentPlayQueueItemEvent, PlayerUIEvent, State>() {
                @Override
                public State call(ActivityLifeCycleEvent event,
                                  CurrentPlayQueueItemEvent currentItemEvent,
                                  PlayerUIEvent playerUIEvent) {
                    return new State(
                            currentItemEvent.getCurrentPlayQueueItem().getUrn(),
                            event.getKind() == ActivityLifeCycleEvent.ON_RESUME_EVENT,
                            adsOperations.isCurrentItemAudioAd(),
                            playerUIEvent.getKind() == PlayerUIEvent.PLAYER_EXPANDED);
                }
            };

    private boolean impressionEventEmitted;

    @Inject
    public VisualAdImpressionOperations(EventBus eventBus, PlayQueueManager playQueueManager,
                                        AccountOperations accountOperations, AdsOperations adsOperations) {
        this.playQueueManager = playQueueManager;
        this.accountOperations = accountOperations;
        this.adsOperations = adsOperations;
        this.activityLifeCycleQueue = eventBus.queue(EventQueue.ACTIVITY_LIFE_CYCLE);
        this.currentTrackQueue = eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                                         .filter(PlayQueueFunctions.IS_TRACK_QUEUE_ITEM);
        this.playerUIEventQueue = eventBus.queue(EventQueue.PLAYER_UI);
    }

    public Observable<TrackingEvent> trackImpression() {
        return Observable
                .combineLatest(
                        activityLifeCycleQueue,
                        currentTrackQueue.doOnNext(unlockCurrentImpression),
                        playerUIEventQueue,
                        combineFunction)
                .filter(isAdVisible)
                .map(toTrackingEvent)
                .doOnNext(lockCurrentImpression);
    }

    private static final class State {
        private final Urn currentTrackUrn;
        private final boolean isAppInForeground;
        private final boolean currentTrackIsAnAudioAd;
        private final boolean playerIsExpanding;

        public State(Urn currentTrackUrn,
                     boolean isAppInForeground,
                     boolean currentTrackIsAnAudioAd,
                     boolean playerIsExpanding) {
            this.currentTrackUrn = currentTrackUrn;
            this.isAppInForeground = isAppInForeground;
            this.currentTrackIsAnAudioAd = currentTrackIsAnAudioAd;
            this.playerIsExpanding = playerIsExpanding;
        }
    }
}
