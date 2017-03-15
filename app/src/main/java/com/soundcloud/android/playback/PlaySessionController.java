package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.MISSING_PLAYABLE_TRACKS;
import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.UNSKIPPABLE;

import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.PlayableAdData;
import com.soundcloud.android.analytics.performance.MetricKey;
import com.soundcloud.android.analytics.performance.MetricParams;
import com.soundcloud.android.analytics.performance.MetricType;
import com.soundcloud.android.analytics.performance.PerformanceMetric;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;


@Singleton
public class PlaySessionController {

    public static final long SEEK_POSITION_RESET = 0L;

    private static final String TAG = "PlaySessionController";
    private static final long PROGRESS_THRESHOLD_FOR_TRACK_CHANGE = TimeUnit.SECONDS.toMillis(3L);
    private static final String SKIP_ORIGIN = "controller";

    private final EventBus eventBus;
    private final AdsOperations adsOperations;

    private final AdsController adsController;
    private final PlayQueueManager playQueueManager;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final CastConnectionHelper castConnectionHelper;
    private final PerformanceMetricsEngine performanceMetricsEngine;

    private final Provider<PlaybackStrategy> playbackStrategyProvider;
    private final PlaybackToastHelper playbackToastHelper;
    private final PlaybackServiceController playbackServiceController;

    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    public PlaySessionController(EventBus eventBus,
                                 AdsOperations adsOperations,
                                 AdsController adsController,
                                 PlayQueueManager playQueueManager,
                                 PlaySessionStateProvider playSessionStateProvider,
                                 CastConnectionHelper castConnectionHelper,
                                 Provider<PlaybackStrategy> playbackStrategyProvider,
                                 PlaybackToastHelper playbackToastHelper,
                                 PlaybackServiceController playbackServiceController,
                                 PerformanceMetricsEngine performanceMetricsEngine) {
        this.eventBus = eventBus;
        this.adsOperations = adsOperations;
        this.adsController = adsController;
        this.playQueueManager = playQueueManager;
        this.playbackStrategyProvider = playbackStrategyProvider;
        this.playbackToastHelper = playbackToastHelper;
        this.playbackServiceController = playbackServiceController;
        this.playSessionStateProvider = playSessionStateProvider;
        this.castConnectionHelper = castConnectionHelper;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, new PlayQueueTrackSubscriber());
    }

    public void reloadQueueAndShowPlayerIfEmpty() {
        if (playQueueManager.isQueueEmpty()) {
            subscription.unsubscribe();
            subscription = playQueueManager.loadPlayQueueAsync()
                                           .doOnNext(playQueue -> eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.showPlayer()))
                                           .subscribe(new DefaultSubscriber<>());
        }
    }

    public void togglePlayback() {
        if (playSessionStateProvider.wasLastStateACastDisconnection()) {
            playCurrent();
        } else if (isPlayingCurrentPlayQueueItem()) {
            if (playSessionStateProvider.isInErrorState()) {
                playCurrent();
            } else {
                playbackStrategyProvider.get().togglePlayback();
            }
        } else {
            playCurrent();
        }
    }

    public boolean isPlayingCurrentPlayQueueItem() {
        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
        return !currentPlayQueueItem.isEmpty() && playSessionStateProvider.isCurrentlyPlaying(currentPlayQueueItem.getUrn());
    }

    public void play() {
        if (isPlayingCurrentPlayQueueItem()) {
            playbackStrategyProvider.get().resume();
        } else {
            playCurrent();
        }
    }

    public void pause() {
        playbackStrategyProvider.get().pause();
    }

    public void fadeAndPause() {
        playbackStrategyProvider.get().fadeAndPause();
    }

    public void seek(long position) {
        if (!shouldDisableSkipping()) {
            if (isPlayingCurrentPlayQueueItem()) {
                playbackStrategyProvider.get().seek(position);
            } else {
                playQueueManager.saveCurrentPosition();
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
                if (playQueueManager.hasPreviousItem()) {
                    startMeasuringTimeToSkip();
                }
                publishSkipEventIfAd();
                playQueueManager.moveToPreviousPlayableItem();
            }
        }
    }

    public void nextTrack() {
        if (shouldDisableSkipping()) {
            playbackToastHelper.showUnskippableAdToast();
        } else {
            if (playQueueManager.hasNextItem()) {
                startMeasuringTimeToSkip();
            }
            publishSkipEventIfAd();
            playQueueManager.moveToNextPlayableItem();
        }
    }

    private void startMeasuringTimeToSkip() {
        if (playSessionStateProvider.isPlaying()) {
            MetricParams params = new MetricParams().putString(MetricKey.SKIP_ORIGIN, SKIP_ORIGIN);
            performanceMetricsEngine.startMeasuring(PerformanceMetric.builder()
                                                                     .metricType(MetricType.TIME_TO_SKIP)
                                                                     .metricParams(params)
                                                                     .build());
        }
    }

    private boolean shouldDisableSkipping() {
        if (adsOperations.isCurrentItemAd()) {
            final PlayableAdData ad = (PlayableAdData) playQueueManager.getCurrentPlayQueueItem().getAdData().get();
            final boolean adIsNotSkippable = !ad.isSkippable();
            final boolean waitingForAdToStart = !isPlayingCurrentPlayQueueItem();
            final boolean haveNotReachedSkippableCheckpoint = playSessionStateProvider.getLastProgressEvent()
                                                                                      .getPosition() < AdConstants.UNSKIPPABLE_TIME_MS;
            return adIsNotSkippable || waitingForAdToStart || haveNotReachedSkippableCheckpoint;
        } else {
            return false;
        }
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
        if (playQueue.isEmpty()) {
            return Observable.just(PlaybackResult.error(MISSING_PLAYABLE_TRACKS));
        } else if (shouldDisableSkipping()) {
            return Observable.just(PlaybackResult.error(UNSKIPPABLE));
        } else {
            return playbackStrategyProvider.get()
                                           .setNewQueue(playQueue, initialTrack, startPosition, playSessionSource)
                                           .doOnSubscribe(() -> subscription.unsubscribe())
                                           .doOnNext(playbackResult -> playCurrent());
        }
    }

    private void publishSkipEventIfAd() {
        final Optional<AdData> adData = adsOperations.getCurrentTrackAdData();
        if (adsOperations.isCurrentItemAd()) {
            eventBus.publish(EventQueue.TRACKING,
                             UIEvent.fromSkipAdClick((PlayableAdData) adData.get(),
                                                     playQueueManager.getCurrentTrackSourceInfo()));
        }
    }

    void playCurrent() {
        subscription.unsubscribe();
        Observable<Void> playCurrentObservable = playQueueManager.isQueueEmpty()
                                                 ? playQueueManager.loadPlayQueueAsync().flatMap(playQueueItems -> playbackStrategyProvider.get().playCurrent())
                                                 : playbackStrategyProvider.get().playCurrent();

        subscription = playCurrentObservable.subscribe(new PlayCurrentSubscriber());
    }

    public void resetPlaySession() {
        playbackServiceController.resetPlaybackService();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
    }

    private class PlayQueueTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {

        private Urn lastKnownPlayQueueTrackUrn = Urn.NOT_SET;

        @Override
        public void onNext(CurrentPlayQueueItemEvent event) {
            final PlayQueueItem playQueueItem = event.getCurrentPlayQueueItem();
            if (playQueueItem.isTrack()) {
                if (castConnectionHelper.isCasting()) {
                    onNextTrackWhileCasting(event, playQueueItem);
                } else if (shouldPlayTrack(playQueueItem.getUrn(), event) || playSessionStateProvider.isInErrorState()) {
                    playSessionStateProvider.clearLastProgressForItem(playQueueItem.getUrn());
                    playCurrent();
                }
            } else if (playQueueItem.isAd()) {
                if (playSessionStateProvider.isPlaying()) {
                    playCurrent();
                }
            }
            lastKnownPlayQueueTrackUrn = playQueueItem.getUrnOrNotSet();
        }

        private void onNextTrackWhileCasting(CurrentPlayQueueItemEvent event, PlayQueueItem playQueueItem) {
            if (event.isRepeat() || !lastKnownPlayQueueTrackUrn.equals(playQueueItem.getUrn())) {
                playCurrent();
            }
        }

        private boolean shouldPlayTrack(Urn newTrack, CurrentPlayQueueItemEvent event) {
            return playSessionStateProvider.isPlaying()
                    && (event.isRepeat() || !lastKnownPlayQueueTrackUrn.equals(newTrack));
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
