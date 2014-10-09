package com.soundcloud.android.ads;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LeaveBehindEvent;
import com.soundcloud.android.events.LeaveBehindImpressionEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.rx.eventbus.EventBus;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func4;
import rx.subjects.Subject;

import javax.inject.Inject;

public class LeaveBehindImpressionOperations {
    private final Subject<ActivityLifeCycleEvent, ActivityLifeCycleEvent> activityLifeCycleQueue;
    private final Subject<CurrentPlayQueueTrackEvent, CurrentPlayQueueTrackEvent> currentTrackQueue;
    private final Subject<PlayerUIEvent, PlayerUIEvent> playerUIEventQueue;
    private final Subject<LeaveBehindEvent, LeaveBehindEvent> leaveBehindEventQueue;

    private final Func1<State, TrackingEvent> toTrackingEvent = new Func1<State, TrackingEvent>() {
        @Override
        public LeaveBehindImpressionEvent call(State state) {
            return new LeaveBehindImpressionEvent(System.currentTimeMillis());
        }
    };

    private final Action1<TrackingEvent> lockCurrentImpression = new Action1<TrackingEvent>() {
        @Override
        public void call(TrackingEvent event) {
            impressionEventEmitted = true;
        }
    };

    private final Action1<CurrentPlayQueueTrackEvent> unlockCurrentImpression = new Action1<CurrentPlayQueueTrackEvent>() {
        @Override
        public void call(CurrentPlayQueueTrackEvent currentPlayQueueTrackEvent) {
            impressionEventEmitted = false;
        }
    };

    private final Func1<State, Boolean> isLeaveBehindVisible = new Func1<State, Boolean>() {
        @Override
        public Boolean call(State state) {
            return !impressionEventEmitted && state.leaveBehindIsVisible && state.playerIsExpanding && state.isAppInForeground;
        }
    };

    private final Func4<LeaveBehindEvent, ActivityLifeCycleEvent, CurrentPlayQueueTrackEvent, PlayerUIEvent, State> combineFunction =
            new Func4<LeaveBehindEvent, ActivityLifeCycleEvent, CurrentPlayQueueTrackEvent, PlayerUIEvent, State>() {
                @Override
                public State call(LeaveBehindEvent leaveBehindEvent,
                                  ActivityLifeCycleEvent event,
                                  CurrentPlayQueueTrackEvent currentPlayQueueTrackEvent,
                                  PlayerUIEvent playerUIEvent) {
                    return new State(
                            leaveBehindEvent.getKind() == LeaveBehindEvent.SHOWN,
                            event.getKind() == ActivityLifeCycleEvent.ON_RESUME_EVENT,
                            playerUIEvent.getKind() == PlayerUIEvent.PLAYER_EXPANDED);
                }
            };

    private boolean impressionEventEmitted;

    @Inject
    public LeaveBehindImpressionOperations(EventBus eventBus) {
        this.activityLifeCycleQueue = eventBus.queue(EventQueue.ACTIVITY_LIFE_CYCLE);
        this.currentTrackQueue = eventBus.queue(EventQueue.PLAY_QUEUE_TRACK);
        this.playerUIEventQueue = eventBus.queue(EventQueue.PLAYER_UI);
        this.leaveBehindEventQueue = eventBus.queue(EventQueue.LEAVE_BEHIND);
    }

    public Observable<TrackingEvent> trackImpression() {
        return Observable
                .combineLatest(
                        leaveBehindEventQueue,
                        activityLifeCycleQueue,
                        currentTrackQueue.doOnNext(unlockCurrentImpression),
                        playerUIEventQueue,
                        combineFunction)
                .filter(isLeaveBehindVisible)
                .map(toTrackingEvent)
                .doOnNext(lockCurrentImpression);
    }

    private static final class State {
        private final boolean leaveBehindIsVisible;
        private final boolean isAppInForeground;
        private final boolean playerIsExpanding;

        public State(boolean leaveBehindIsVisible, boolean isAppInForeground, boolean playerIsExpanding) {
            this.isAppInForeground = isAppInForeground;
            this.leaveBehindIsVisible = leaveBehindIsVisible;
            this.playerIsExpanding = playerIsExpanding;
        }
    }
}
