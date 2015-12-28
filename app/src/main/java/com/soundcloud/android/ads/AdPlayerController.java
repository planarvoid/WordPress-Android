package com.soundcloud.android.ads;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func2;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AdPlayerController extends DefaultActivityLightCycle<AppCompatActivity> {
    private final EventBus eventBus;
    private final AdsOperations adsOperations;

    private Subscription subscription = RxUtils.invalidSubscription();
    private Urn lastSeenAdUrn = Urn.NOT_SET;

    private final Action1<PlayerState> setAdHasBeenSeen = new Action1<PlayerState>() {
        @Override
        public void call(PlayerState playerState) {
            if (isPlayerExpandedWithAd(playerState)) {
                lastSeenAdUrn = playerState.itemUrn;
            } else if (!isDifferentTrack(playerState)) {
                lastSeenAdUrn = Urn.NOT_SET;
            }
        }

        private boolean isPlayerExpandedWithAd(PlayerState playerState) {
            return playerState.playerUIEventKind == PlayerUIEvent.PLAYER_EXPANDED && playerState.isAd;
        }

        private boolean isDifferentTrack(PlayerState playerState) {
            return lastSeenAdUrn.equals(playerState.itemUrn);
        }
    };

    private final Func2<CurrentPlayQueueItemEvent, PlayerUIEvent, PlayerState> toPlayerState = new Func2<CurrentPlayQueueItemEvent, PlayerUIEvent, PlayerState>() {
        @Override
        public PlayerState call(CurrentPlayQueueItemEvent currentItemEvent, PlayerUIEvent playerUIEvent) {
            return new PlayerState(adsOperations.isCurrentItemAd(),
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
                        eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM),
                        eventBus.queue(EventQueue.PLAYER_UI),
                        toPlayerState)
                .doOnNext(setAdHasBeenSeen)
                .subscribe(new PlayQueueSubscriber());
    }

    @Override
    public void onPause(AppCompatActivity activity) {
        subscription.unsubscribe();
    }

    private final class PlayQueueSubscriber extends DefaultSubscriber<PlayerState> {
        @Override
        public void onNext(PlayerState event) {
            if (adsOperations.isCurrentItemVideoAd()) {
                eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.lockPlayerExpanded());
                lastSeenAdUrn = event.itemUrn;
            } else {
                eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.unlockPlayer());
                if (adsOperations.isCurrentItemAudioAd() && !lastSeenAdUrn.equals(event.itemUrn)) {
                    eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
                    eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayerOpen(UIEvent.METHOD_AD_PLAY));
                    lastSeenAdUrn = event.itemUrn;
                }
            }
        }
    }

    private static class PlayerState {
        private final boolean isAd;
        private final Urn itemUrn;
        private final int playerUIEventKind;

        public PlayerState(boolean isAd, Urn itemUrn, int playerUIEventKind) {
            this.isAd = isAd;
            this.itemUrn = itemUrn;
            this.playerUIEventKind = playerUIEventKind;
        }
    }
}
