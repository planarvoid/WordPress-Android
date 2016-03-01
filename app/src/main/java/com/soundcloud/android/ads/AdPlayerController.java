package com.soundcloud.android.ads;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.VideoQueueItem;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.subscriptions.CompositeSubscription;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.ref.WeakReference;

@Singleton
public class AdPlayerController extends DefaultActivityLightCycle<AppCompatActivity> {

    private final EventBus eventBus;
    private final AdsOperations adsOperations;
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
            return playerState.playerUIEventKind == PlayerUIEvent.PLAYER_EXPANDED && playerState.isAd;
        }

        private boolean isDifferentTrack(PlayerState playerState) {
            return playerState.playQueueItem.isEmpty() || lastSeenAdUrn.equals(playerState.playQueueItem.getUrn());
        }
    };

    private final Func2<CurrentPlayQueueItemEvent, PlayerUIEvent, PlayerState> toPlayerState = new Func2<CurrentPlayQueueItemEvent, PlayerUIEvent, PlayerState>() {
        @Override
        public PlayerState call(CurrentPlayQueueItemEvent currentItemEvent, PlayerUIEvent playerUIEvent) {
            return new PlayerState(adsOperations.isCurrentItemAd(),
                    currentItemEvent.getCurrentPlayQueueItem(),
                    playerUIEvent.getKind());
        }
    };

    @Inject
    public AdPlayerController(final EventBus eventBus, AdsOperations adsOperations, PlaySessionController playSessionController) {
        this.eventBus = eventBus;
        this.adsOperations = adsOperations;
        this.playSessionController = playSessionController;
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        subscription = new CompositeSubscription(Observable
                .combineLatest(
                        eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM),
                        eventBus.queue(EventQueue.PLAYER_UI),
                        toPlayerState)
                .doOnNext(setAdHasBeenSeen)
                .subscribe(new PlayQueueSubscriber(activity)),
                eventBus.queue(EventQueue.PLAYER_COMMAND).subscribe(new PlayerCommandSubscriber(activity)));
    }

    @Override
    public void onPause(AppCompatActivity activity) {
        if (adsOperations.isCurrentItemVideoAd() && !activity.isChangingConfigurations()) {
            playSessionController.pause();
        }
        subscription.unsubscribe();
    }

    private class PlayerCommandSubscriber extends DefaultSubscriber<PlayerUICommand> {

        final WeakReference<Activity> currentActivityRef;

        PlayerCommandSubscriber(Activity activity) {
            currentActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void onNext(PlayerUICommand event) {
            final Activity currentActivity = currentActivityRef.get();
            if (currentActivity != null && adsOperations.isCurrentItemVideoAd()) {
                if (event.isForceLandscape()) {
                    currentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                } else if (event.isForcePortrait()) {
                    currentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
        }
    }

    private final class PlayQueueSubscriber extends DefaultSubscriber<PlayerState> {

        final WeakReference<Activity> currentActivityRef;

        PlayQueueSubscriber(Activity activity) {
           currentActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void onNext(PlayerState event) {
            final PlayQueueItem currentItem = event.playQueueItem;
            if (currentItem.isVideo()) {
                lockOrientationInPortraitIfVertical((VideoQueueItem) currentItem);
                eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.lockPlayerExpanded());
                lastSeenAdUrn = currentItem.getUrn();
            } else {
                unlockOrientationAndPlayer();
                if (adsOperations.isCurrentItemAudioAd() && !lastSeenAdUrn.equals(currentItem.getUrn())) {
                    eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
                    eventBus.publish(EventQueue.TRACKING, UIEvent.fromPlayerOpen(UIEvent.METHOD_AD_PLAY));
                    lastSeenAdUrn = currentItem.getUrn();
                }
            }
        }

        private void lockOrientationInPortraitIfVertical(VideoQueueItem videoItem) {
            final Activity currentActivity = currentActivityRef.get();
            if (currentActivity != null && videoItem.isVerticalVideo()) {
                currentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }

        private void unlockOrientationAndPlayer() {
            final Activity currentActivity = currentActivityRef.get();
            if (currentActivity != null) {
                currentActivityRef.get().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.unlockPlayer());
        }
    }

    private static class PlayerState {
        private final boolean isAd;
        private final PlayQueueItem playQueueItem;
        private final int playerUIEventKind;

        public PlayerState(boolean isAd, PlayQueueItem playQueueItem, int playerUIEventKind) {
            this.isAd = isAd;
            this.playQueueItem = playQueueItem;
            this.playerUIEventKind = playerUIEventKind;
        }
    }
}
