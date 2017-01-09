package com.soundcloud.android.ads;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.VisualAdImpressionEvent;
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
    private final Subject<ActivityLifeCycleEvent, ActivityLifeCycleEvent> activityLifeCycleQueue;
    private final Observable<CurrentPlayQueueItemEvent> currentAudioAdQueue;
    private final Subject<PlayerUIEvent, PlayerUIEvent> playerUIEventQueue;

    private final Func1<State, TrackingEvent> toTrackingEvent = new Func1<State, TrackingEvent>() {
        @Override
        public VisualAdImpressionEvent call(State state) {
            return new VisualAdImpressionEvent(
                    (AudioAd) state.adData,
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
            return !impressionEventEmitted && state.playerIsExpanding && state.isAppInForeground;
        }
    };

    private final Func3<ActivityLifeCycleEvent, CurrentPlayQueueItemEvent, PlayerUIEvent, State> combineFunction =
            (event, currentItemEvent, playerUIEvent) -> new State(currentItemEvent.getCurrentPlayQueueItem().getAdData().get(),
                             event.getKind() == ActivityLifeCycleEvent.ON_RESUME_EVENT,
                             playerUIEvent.getKind() == PlayerUIEvent.PLAYER_EXPANDED);

    private boolean impressionEventEmitted;

    @Inject
    public VisualAdImpressionOperations(EventBus eventBus,
                                        PlayQueueManager playQueueManager,
                                        AccountOperations accountOperations) {
        this.playQueueManager = playQueueManager;
        this.accountOperations = accountOperations;
        this.activityLifeCycleQueue = eventBus.queue(EventQueue.ACTIVITY_LIFE_CYCLE);
        this.currentAudioAdQueue = eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM)
                                           .filter(PlayQueueFunctions.IS_AUDIO_AD_QUEUE_ITEM);
        this.playerUIEventQueue = eventBus.queue(EventQueue.PLAYER_UI);
    }

    public Observable<TrackingEvent> trackImpression() {
        return Observable
                .combineLatest(
                        activityLifeCycleQueue,
                        currentAudioAdQueue.doOnNext(unlockCurrentImpression),
                        playerUIEventQueue,
                        combineFunction)
                .filter(isAdVisible)
                .map(toTrackingEvent)
                .doOnNext(lockCurrentImpression);
    }

    private static final class State {
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
