package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.UNSKIPPABLE;

import com.soundcloud.android.PlaybackServiceInitiator;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;


@Singleton
public class PlaySessionController {

    private static final String TAG = "PlaySessionController";

    private static final long PROGRESS_THRESHOLD_FOR_TRACK_CHANGE = TimeUnit.SECONDS.toMillis(3L);
    private static final long SEEK_POSITION_RESET = 0L;

    private final EventBus eventBus;
    private final AdsOperations adsOperations;

    private final AdsController adsController;
    private final PlayQueueManager playQueueManager;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final CastConnectionHelper castConnectionHelper;

    private final Provider<PlaybackStrategy> playbackStrategyProvider;
    private final PlaybackToastHelper playbackToastHelper;
    private final AccountOperations accountOperations;
    private final PlaybackServiceInitiator playbackServiceInitiator;

    private Subscription subscription = RxUtils.invalidSubscription();

    private final Action0 stopLoadingPreviousTrack = new Action0() {
        @Override
        public void call() {
            subscription.unsubscribe();
        }
    };

    private final Action1<PlaybackResult> playCurrentTrack = new Action1<PlaybackResult>() {
        @Override
        public void call(PlaybackResult playbackResult) {
            playCurrent();
        }
    };

    private final Func1<PlayQueue, Observable<Void>> toPlayCurrent = new Func1<PlayQueue, Observable<Void>>() {
        @Override
        public Observable<Void> call(PlayQueue playQueueItems) {
            return playbackStrategyProvider.get().playCurrent();
        }
    };

    private final Action1<PlayQueue> showPlayer = new Action1<PlayQueue>() {
        @Override
        public void call(PlayQueue ignore) {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.showPlayer());
        }
    };

    @Inject
    public PlaySessionController(EventBus eventBus,
                                 AdsOperations adsOperations,
                                 AdsController adsController,
                                 PlayQueueManager playQueueManager,
                                 PlaySessionStateProvider playSessionStateProvider,
                                 CastConnectionHelper castConnectionHelper,
                                 Provider<PlaybackStrategy> playbackStrategyProvider,
                                 PlaybackToastHelper playbackToastHelper,
                                 AccountOperations accountOperations,
                                 PlaybackServiceInitiator playbackServiceInitiator) {
        this.eventBus = eventBus;
        this.adsOperations = adsOperations;
        this.adsController = adsController;
        this.playQueueManager = playQueueManager;
        this.playbackStrategyProvider = playbackStrategyProvider;
        this.playbackToastHelper = playbackToastHelper;
        this.accountOperations = accountOperations;
        this.playbackServiceInitiator = playbackServiceInitiator;
        this.playSessionStateProvider = playSessionStateProvider;
        this.castConnectionHelper = castConnectionHelper;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, new PlayQueueTrackSubscriber());
    }

    public void reloadQueueAndShowPlayerIfEmpty() {
        if (playQueueManager.isQueueEmpty()) {
            subscription.unsubscribe();
            subscription = playQueueManager.loadPlayQueueAsync()
                    .doOnNext(showPlayer)
                    .subscribe(new DefaultSubscriber<PlayQueue>());
        }
    }

    public void togglePlayback() {
        if (playSessionStateProvider.isPlayingCurrentPlayQueueItem()) {
            if (playSessionStateProvider.isInErrorState()) {
                playCurrent();
            } else {
                playbackStrategyProvider.get().togglePlayback();
            }
        } else {
            playCurrent();
        }
    }

    public void play() {
        playbackStrategyProvider.get().resume();
    }

    public void pause() {
        playbackStrategyProvider.get().pause();
    }

    public void seek(long position) {
        if (!shouldDisableSkipping()) {
            if (playSessionStateProvider.isPlayingCurrentPlayQueueItem()) {
                playbackStrategyProvider.get().seek(position);
            } else {
                playQueueManager.saveCurrentProgress(position);
            }
        }
    }

    public void previousTrack() {
        if (shouldDisableSkipping()) {
            playbackToastHelper.showUnskippableAdToast();
        } else {
            if (playSessionStateProvider.getLastProgressEvent().getPosition() >= PROGRESS_THRESHOLD_FOR_TRACK_CHANGE
                    && !adsOperations.isCurrentItemAd()) {
                seek(SEEK_POSITION_RESET);
            } else {
                publishSkipEventIfAd();
                playQueueManager.moveToPreviousPlayableItem();
            }
        }
    }

    public void nextTrack() {
        if (shouldDisableSkipping()) {
            playbackToastHelper.showUnskippableAdToast();
        } else {
            publishSkipEventIfAd();
            playQueueManager.moveToNextPlayableItem();
        }
    }

    private boolean shouldDisableSkipping() {
        return adsOperations.isCurrentItemAd() &&
                playSessionStateProvider.getLastProgressEventForCurrentPlayQueueItem().getPosition() < AdConstants.UNSKIPPABLE_TIME_MS;
    }

    public void setCurrentPlayQueueItem(PlayQueueItem playQueueItem) {
        if (!playQueueManager.getCurrentPlayQueueItem().equals(playQueueItem)) {
            adsController.publishAdDeliveryEventIfUpcoming();
            publishSkipEventIfAd();
            playQueueManager.setCurrentPlayQueueItem(playQueueItem);
        }
    }

    public Observable<PlaybackResult> playNewQueue(PlayQueue playQueue, Urn initialTrack, int startPosition,
                                                   PlaySessionSource playSessionSource) {
        if (shouldDisableSkipping()) {
            return Observable.just(PlaybackResult.error(UNSKIPPABLE));
        } else {
            return playbackStrategyProvider.get()
                    .setNewQueue(playQueue, initialTrack, startPosition, playSessionSource)
                    .doOnSubscribe(stopLoadingPreviousTrack)
                    .doOnNext(playCurrentTrack);
        }
    }

    private void publishSkipEventIfAd() {
        final Optional<AdData> adData = adsOperations.getCurrentTrackAdData();
        if (adsOperations.isCurrentItemAudioAd()) {
            final UIEvent audioSkipEvent = UIEvent.fromSkipAudioAdClick((AudioAd) adData.get(),
                    playQueueManager.getCurrentPlayQueueItem().getUrn(),
                    accountOperations.getLoggedInUserUrn(),
                    playQueueManager.getCurrentTrackSourceInfo());
            eventBus.publish(EventQueue.TRACKING, audioSkipEvent);
        } else if (adsOperations.isCurrentItemVideoAd()) {
            eventBus.publish(EventQueue.TRACKING, UIEvent.fromSkipVideoAdClick((VideoAd) adData.get(), playQueueManager.getCurrentTrackSourceInfo()));
        }
    }

    void playCurrent() {
        subscription.unsubscribe();
        Observable<Void> playCurrentObservable = playQueueManager.isQueueEmpty()
                ? playQueueManager.loadPlayQueueAsync().flatMap(toPlayCurrent)
                : playbackStrategyProvider.get().playCurrent();

        subscription = playCurrentObservable.subscribe(new PlayCurrentSubscriber());
    }

    public void resetPlaySession() {
        playbackServiceInitiator.resetPlaybackService();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
    }

    private class PlayQueueTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {

        private Urn lastKnownPlayQueueTrackUrn = Urn.NOT_SET;

        @Override
        public void onNext(CurrentPlayQueueItemEvent event) {
            subscription.unsubscribe();

            final PlayQueueItem playQueueItem = event.getCurrentPlayQueueItem();
            if (playQueueItem.isTrack()) {
                if (castConnectionHelper.isCasting()) {
                    if (playSessionStateProvider.isPlaying() && !lastKnownPlayQueueTrackUrn.equals(playQueueItem.getUrn())) {
                        playCurrent();
                    }
                } else if (playSessionStateProvider.isPlaying()) {
                    playCurrent();
                }

            } else if (playQueueItem.isVideo()) {
                if (playSessionStateProvider.isPlaying()) {
                    playCurrent();
                }
            }
            lastKnownPlayQueueTrackUrn = playQueueItem.getUrnOrNotSet();
        }
    }

    private class PlayCurrentSubscriber extends DefaultSubscriber<Void> {
        @Override
        public void onError(Throwable e) {
            if (e instanceof BlockedTrackException) {
                pause();
                Log.e(TAG, "Not playing blocked track", e);
            } else {
                super.onError(e);
            }
        }
    }
}
