package com.soundcloud.android.ads;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AudioAdCompanionImpressionEvent;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.tracks.TrackUrn;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func3;
import rx.subjects.Subject;

import javax.inject.Inject;

public class AdCompanionImpressionController {
    private final PlayQueueManager playQueueManager;
    private final AccountOperations accountOperations;
    private final Subject<ActivityLifeCycleEvent, ActivityLifeCycleEvent> activityLifeCycleEventQueue;
    private final Subject<CurrentPlayQueueTrackEvent, CurrentPlayQueueTrackEvent> currentPlayQueueTrackEventQueue;
    private final Subject<PlayerUIEvent, PlayerUIEvent> playerUIEventQueue;

    private final Func1<State, AudioAdCompanionImpressionEvent> toCompanionImpressionEvent = new Func1<State, AudioAdCompanionImpressionEvent>() {
        @Override
        public AudioAdCompanionImpressionEvent call(State state) {
            return new AudioAdCompanionImpressionEvent(playQueueManager.getAudioAd(), state.currentTrackUrn, accountOperations.getLoggedInUserUrn());
        }
    };

    private final Action1<AudioAdCompanionImpressionEvent> lockCurrentCompanionImpression = new Action1<AudioAdCompanionImpressionEvent>() {
        @Override
        public void call(AudioAdCompanionImpressionEvent audioAdCompanionImpressionEvent) {
            currentCompanionImpressionEventEmitted = true;
        }
    };

    private final Action1<CurrentPlayQueueTrackEvent> unlockCurrentCompanionImpression = new Action1<CurrentPlayQueueTrackEvent>() {
        @Override
        public void call(CurrentPlayQueueTrackEvent currentPlayQueueTrackEvent) {
            currentCompanionImpressionEventEmitted = false;
        }
    };

    private final Func1<State, Boolean> newCompanionIsVisible = new Func1<State, Boolean>() {
        @Override
        public Boolean call(State state) {
            return !currentCompanionImpressionEventEmitted && state.currentTrackIsAnAudioAd && state.playerIsExpanding && state.isAppInForeground;
        }
    };

    private final Func3<ActivityLifeCycleEvent, CurrentPlayQueueTrackEvent, PlayerUIEvent, State> combineFunction = new Func3<ActivityLifeCycleEvent, CurrentPlayQueueTrackEvent, PlayerUIEvent, State>() {
        @Override
        public State call(ActivityLifeCycleEvent event, CurrentPlayQueueTrackEvent currentPlayQueueTrackEvent, PlayerUIEvent playerUIEvent) {
            return new State(
                    currentPlayQueueTrackEvent.getCurrentTrackUrn(),
                    event.getKind() == ActivityLifeCycleEvent.ON_RESUME_EVENT,
                    playQueueManager.isCurrentTrackAudioAd(),
                    playerUIEvent.isExpanding());
        }
    };

    private boolean currentCompanionImpressionEventEmitted = false;

    @Inject
    public AdCompanionImpressionController(final EventBus eventBus, final PlayQueueManager playQueueManager, final AccountOperations accountOperations) {
        this.playQueueManager = playQueueManager;
        this.accountOperations = accountOperations;
        this.activityLifeCycleEventQueue = eventBus.queue(EventQueue.ACTIVITY_LIFE_CYCLE);
        this.currentPlayQueueTrackEventQueue = eventBus.queue(EventQueue.PLAY_QUEUE_TRACK);
        this.playerUIEventQueue = eventBus.queue(EventQueue.PLAYER_UI);
    }

    public Observable<AudioAdCompanionImpressionEvent> companionImpressionEvent() {
        return Observable
                .combineLatest(
                        activityLifeCycleEventQueue,
                        currentPlayQueueTrackEventQueue.doOnNext(unlockCurrentCompanionImpression),
                        playerUIEventQueue,
                        combineFunction)
                .filter(newCompanionIsVisible)
                .map(toCompanionImpressionEvent)
                .doOnNext(lockCurrentCompanionImpression);
    }

    private static final class State {
        private final TrackUrn currentTrackUrn;
        private final boolean isAppInForeground;
        private final boolean currentTrackIsAnAudioAd;
        private final boolean playerIsExpanding;

        public State(TrackUrn currentTrackUrn, boolean isAppInForeground, boolean currentTrackIsAnAudioAd, boolean playerIsExpanding) {
            this.currentTrackUrn = currentTrackUrn;
            this.isAppInForeground = isAppInForeground;
            this.currentTrackIsAnAudioAd = currentTrackIsAnAudioAd;
            this.playerIsExpanding = playerIsExpanding;
        }
    }
}
