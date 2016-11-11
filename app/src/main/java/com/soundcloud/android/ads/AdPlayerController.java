package com.soundcloud.android.ads;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.playqueue.PlayQueueUIEvent;
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

    private final AdsOperations adsOperations;
    private final EventBus eventBus;
    private final PlaySessionController playSessionController;

    private Subscription subscription = RxUtils.invalidSubscription();
    private Urn lastSeenAdUrn = Urn.NOT_SET;

    private final Action1<PlayerState> setAdHasBeenSeen = new Action1<PlayerState>() {
        @Override
        public void call(PlayerState playerState) {
            if (isPlayerExpandedWithAd(playerState)) {
                lastSeenAdUrn = playerState.playQueueItem.getUrn();
            } else if (!isDifferentTrack(playerState)) {
                lastSeenAdUrn = Urn.NOT_SET;
            }
        }

        private boolean isPlayerExpandedWithAd(PlayerState playerState) {
            return playerState.playerUIEventKind == PlayerUIEvent.PLAYER_EXPANDED && playerState.playQueueItem.isAd();
        }

        private boolean isDifferentTrack(PlayerState playerState) {
            return playerState.playQueueItem.isEmpty() || lastSeenAdUrn.equals(playerState.playQueueItem.getUrn());
        }
    };

    private final Func2<CurrentPlayQueueItemEvent, PlayerUIEvent, PlayerState> toPlayerState = new Func2<CurrentPlayQueueItemEvent, PlayerUIEvent, PlayerState>() {
        @Override
        public PlayerState call(CurrentPlayQueueItemEvent currentItemEvent, PlayerUIEvent playerUIEvent) {
            return new PlayerState(currentItemEvent.getCurrentPlayQueueItem(), playerUIEvent.getKind());
        }
    };

    @Inject
    public AdPlayerController(final EventBus eventBus,
                              AdsOperations adsOperations,
                              PlaySessionController playSessionController) {
        this.eventBus = eventBus;
        this.adsOperations = adsOperations;
        this.playSessionController = playSessionController;
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
        if (adsOperations.isCurrentItemVideoAd() && !activity.isChangingConfigurations()) {
            playSessionController.pause();
        }
        subscription.unsubscribe();
    }

    private final class PlayQueueSubscriber extends DefaultSubscriber<PlayerState> {

        @Override
        public void onNext(PlayerState event) {
            final PlayQueueItem currentItem = event.playQueueItem;

            if (currentItem.isAd()) {
                eventBus.publish(EventQueue.PLAY_QUEUE_UI, PlayQueueUIEvent.createHideEvent());
            }

            if (currentItem.isVideoAd()) {
                eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.lockPlayerExpanded());
                lastSeenAdUrn = currentItem.getUrn();
            } else {
                eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.unlockPlayer());
                if (adsOperations.isCurrentItemAudioAd() && shouldExpandAudioAd()
                        && !lastSeenAdUrn.equals(currentItem.getUrn())) {
                    expandAudioAd(currentItem);
                }
            }
        }
    }

    private boolean shouldExpandAudioAd() {
        return ((AudioAd) adsOperations.getCurrentTrackAdData().get()).hasCompanion();
    }

    private void expandAudioAd(PlayQueueItem currentItem) {
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
        lastSeenAdUrn = currentItem.getUrn();
    }

    private static class PlayerState {
        private final PlayQueueItem playQueueItem;
        private final int playerUIEventKind;

        public PlayerState(PlayQueueItem playQueueItem, int playerUIEventKind) {
            this.playQueueItem = playQueueItem;
            this.playerUIEventKind = playerUIEventKind;
        }
    }
}
