package com.soundcloud.android.ads;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.DefaultActivityLifeCycle;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackUrn;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AdPlayerController extends DefaultActivityLifeCycle {
    private final EventBus eventBus;
    private final AdsOperations adsOperations;

    private Subscription subscription = Subscriptions.empty();
    private TrackUrn adHasBeenSeen = TrackUrn.NOT_SET;

    private final Func1<State, Boolean> isNewAudioAd = new Func1<State, Boolean>() {
        @Override
        public Boolean call(State state) {
            return state.isAudioAd && !adHasBeenSeen.equals(state.trackUrn);
        }
    };

    private final Action1<State> setAdHasBeenSeen = new Action1<State>() {

        @Override
        public void call(State state) {
            if (isPlayerExpandedWithAd(state)) {
                adHasBeenSeen = state.trackUrn;
            } else if (!isDifferentTrack(state)) {
                adHasBeenSeen = TrackUrn.NOT_SET;
            }
        }

        private boolean isPlayerExpandedWithAd(State state) {
            return state.playerUIEventKind == PlayerUIEvent.PLAYER_EXPANDED && state.isAudioAd;
        }

        private boolean isDifferentTrack(State state) {
            return adHasBeenSeen.equals(state.trackUrn);
        }
    };

    private final Func2<CurrentPlayQueueTrackEvent, PlayerUIEvent, State> combine = new Func2<CurrentPlayQueueTrackEvent, PlayerUIEvent, State>() {
        @Override
        public State call(CurrentPlayQueueTrackEvent currentPlayQueueTrackEvent, PlayerUIEvent playerUIEvent) {
            return new State(adsOperations.isCurrentTrackAudioAd(),
                    currentPlayQueueTrackEvent.getCurrentTrackUrn(),
                    playerUIEvent.getKind());
        }
    };

    @Inject
    public AdPlayerController(final EventBus eventBus, AdsOperations adsOperations) {
        this.eventBus = eventBus;
        this.adsOperations = adsOperations;
    }

    @Override
    public void onResume() {
        subscription = Observable
                .combineLatest(
                        eventBus.queue(EventQueue.PLAY_QUEUE_TRACK),
                        eventBus.queue(EventQueue.PLAYER_UI),
                        combine)
                .doOnNext(setAdHasBeenSeen)
                .filter(isNewAudioAd)
                .subscribe(new PlayQueueSubscriber());
    }

    @Override
    public void onPause() {
        subscription.unsubscribe();
    }

    private final class PlayQueueSubscriber extends DefaultSubscriber<State> {
        @Override
        public void onNext(State event) {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
            eventBus.publish(EventQueue.UI, UIEvent.fromPlayerOpen(UIEvent.METHOD_AD_PLAY));
            adHasBeenSeen = event.trackUrn;
        }
    }

    private static class State {
        private final boolean isAudioAd;
        private final TrackUrn trackUrn;
        private final int playerUIEventKind;

        public State(boolean isAudioAd, TrackUrn trackUrn, int playerUIEventKind) {
            this.isAudioAd = isAudioAd;
            this.trackUrn = trackUrn;
            this.playerUIEventKind = playerUIEventKind;
        }
    }
}
