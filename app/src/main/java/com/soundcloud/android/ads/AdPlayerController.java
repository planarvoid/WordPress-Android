package com.soundcloud.android.ads;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueFunctions;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AdPlayerController extends DefaultActivityLightCycle<AppCompatActivity> {
    private final EventBus eventBus;
    private final AdsOperations adsOperations;

    private Subscription subscription = RxUtils.invalidSubscription();
    private Urn lastSeenAdTrack = Urn.NOT_SET;

    private final Func1<State, Boolean> isNewAudioAd = new Func1<State, Boolean>() {
        @Override
        public Boolean call(State state) {
            return state.isAudioAd && !lastSeenAdTrack.equals(state.trackUrn);
        }
    };

    private final Action1<State> setAdHasBeenSeen = new Action1<State>() {

        @Override
        public void call(State state) {
            if (isPlayerExpandedWithAd(state)) {
                lastSeenAdTrack = state.trackUrn;
            } else if (!isDifferentTrack(state)) {
                lastSeenAdTrack = Urn.NOT_SET;
            }
        }

        private boolean isPlayerExpandedWithAd(State state) {
            return state.playerUIEventKind == PlayerUIEvent.PLAYER_EXPANDED && state.isAudioAd;
        }

        private boolean isDifferentTrack(State state) {
            return lastSeenAdTrack.equals(state.trackUrn);
        }
    };

    private final Func2<CurrentPlayQueueItemEvent, PlayerUIEvent, State> combine = new Func2<CurrentPlayQueueItemEvent, PlayerUIEvent, State>() {
        @Override
        public State call(CurrentPlayQueueItemEvent currentItemEvent, PlayerUIEvent playerUIEvent) {
            return new State(adsOperations.isCurrentTrackAudioAd(),
                    currentItemEvent.getCurrentPlayQueueItem().getUrn(),
                    playerUIEvent.getKind());
        }
    };

    @Inject
    public AdPlayerController(final EventBus eventBus, AdsOperations adsOperations) {
        this.eventBus = eventBus;
        this.adsOperations = adsOperations;
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        subscription = Observable
                .combineLatest(
                        eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM).filter(PlayQueueFunctions.IS_TRACK_QUEUE_ITEM),
                        eventBus.queue(EventQueue.PLAYER_UI),
                        combine)
                .doOnNext(setAdHasBeenSeen)
                .filter(isNewAudioAd)
                .subscribe(new PlayQueueSubscriber());
    }

    @Override
    public void onPause(AppCompatActivity activity) {
        subscription.unsubscribe();
    }

    private final class PlayQueueSubscriber extends DefaultSubscriber<State> {
        @Override
        public void onNext(State event) {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayerOpen(UIEvent.METHOD_AD_PLAY));
            lastSeenAdTrack = event.trackUrn;
        }
    }

    private static class State {
        private final boolean isAudioAd;
        private final Urn trackUrn;
        private final int playerUIEventKind;

        public State(boolean isAudioAd, Urn trackUrn, int playerUIEventKind) {
            this.isAudioAd = isAudioAd;
            this.trackUrn = trackUrn;
            this.playerUIEventKind = playerUIEventKind;
        }
    }
}
