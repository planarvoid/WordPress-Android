package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.MISSING_PLAYABLE_TRACKS;
import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.UNSKIPPABLE;

import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.PlayableAdData;
import com.soundcloud.android.ads.PlayerAdsController;
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
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.rx.observers.DefaultDisposableCompletableObserver;
import com.soundcloud.android.rx.observers.LambdaMaybeObserver;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;

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

    private final EventBusV2 eventBus;
    private final AdsOperations adsOperations;

    private final PlayerAdsController playerAdsController;
    private final PlayQueueManager playQueueManager;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final CastConnectionHelper castConnectionHelper;
    private final PerformanceMetricsEngine performanceMetricsEngine;

    private final Provider<PlaybackStrategy> playbackStrategyProvider;
    private final PlaybackFeedbackHelper playbackFeedbackHelper;
    private final PlaybackServiceController playbackServiceController;
    private final PlaybackProgressRepository playbackProgressRepository;
    private PlayQueueItem lastPlayQueueItem = PlayQueueItem.EMPTY;

    private Disposable disposable = Disposables.disposed();

    @Inject
    public PlaySessionController(EventBusV2 eventBus,
                                 AdsOperations adsOperations,
                                 PlayerAdsController playerAdsController,
                                 PlayQueueManager playQueueManager,
                                 PlaySessionStateProvider playSessionStateProvider,
                                 CastConnectionHelper castConnectionHelper,
                                 Provider<PlaybackStrategy> playbackStrategyProvider,
                                 PlaybackFeedbackHelper playbackFeedbackHelper,
                                 PlaybackServiceController playbackServiceController,
                                 PlaybackProgressRepository playbackProgressRepository,
                                 PerformanceMetricsEngine performanceMetricsEngine) {
        this.eventBus = eventBus;
        this.adsOperations = adsOperations;
        this.playerAdsController = playerAdsController;
        this.playQueueManager = playQueueManager;
        this.playbackStrategyProvider = playbackStrategyProvider;
        this.playbackFeedbackHelper = playbackFeedbackHelper;
        this.playbackServiceController = playbackServiceController;
        this.playSessionStateProvider = playSessionStateProvider;
        this.castConnectionHelper = castConnectionHelper;
        this.playbackProgressRepository = playbackProgressRepository;
        this.performanceMetricsEngine = performanceMetricsEngine;
    }

    public void reloadQueueAndShowPlayerIfEmpty() {
        if (playQueueManager.isQueueEmpty()) {
            disposable.dispose();
            disposable = playQueueManager.loadPlayQueueAsync()
                                         .subscribeWith(LambdaMaybeObserver.onNext(playQueueItems -> eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.showPlayer())));
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

    public boolean isPlaying() {
        return playSessionStateProvider.isPlaying();
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
            playbackProgressRepository.put(playQueueManager.getCurrentPlayQueueItem().getUrn(), position);
            if (isPlayingCurrentPlayQueueItem()) {
                playbackStrategyProvider.get().seek(position);
            } else {
                playQueueManager.saveCurrentPosition();
            }
        }
    }

    public void previousTrack() {
        if (shouldDisableSkipping()) {
            playbackFeedbackHelper.showUnskippableAdFeedback();
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
            playbackFeedbackHelper.showUnskippableAdFeedback();
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
            playerAdsController.publishAdDeliveryEventIfUpcoming();
            publishSkipEventIfAd();
            playQueueManager.setCurrentPlayQueueItem(playQueueItem);
        }
    }

    public Single<PlaybackResult> playNewQueue(PlayQueue playQueue, Urn initialTrack, int startPosition,
                                               PlaySessionSource playSessionSource) {
        if (playQueue.isEmpty()) {
            return Single.just(PlaybackResult.error(MISSING_PLAYABLE_TRACKS));
        } else if (shouldDisableSkipping()) {
            return Single.just(PlaybackResult.error(UNSKIPPABLE));
        } else {
            return playbackStrategyProvider.get()
                                           .setNewQueue(playQueue, initialTrack, startPosition, playSessionSource)
                                           .doOnSubscribe(__ -> disposable.dispose())
                                           .doOnSuccess(playbackResult -> playCurrent());
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
        disposable.dispose();
        Completable playCurrentObservable = playQueueManager.isQueueEmpty()
                                            ? playQueueManager.loadPlayQueueAsync().flatMapCompletable(playQueueItems -> playbackStrategyProvider.get().playCurrent())
                                            : playbackStrategyProvider.get().playCurrent();

        disposable = playCurrentObservable.subscribeWith(new DefaultDisposableCompletableObserver() {
            @Override
            public void onError(Throwable e) {
                if (e instanceof BlockedTrackException) {
                    pause();
                    Log.e(TAG, "Not playing blocked track", e);
                } else if (e instanceof MissingTrackException) {
                    ErrorUtils.handleSilentException(e);
                    nextTrack();
                } else {
                    super.onError(e);
                }
            }
        });
    }

    public void resetPlaySession() {
        playbackServiceController.resetPlaybackService();
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerCollapsed());
    }

    void onPlayQueueItemEvent(CurrentPlayQueueItemEvent event) {
        final PlayQueueItem playQueueItem = event.getCurrentPlayQueueItem();
        if (playQueueItem.isTrack()) {
            playSessionStateProvider.clearLastProgressForItem(playQueueItem.getUrn());
            if (castConnectionHelper.isCasting()) {
                onNextTrackWhileCasting(event, playQueueItem);
            } else if (shouldPlayTrack(event, playQueueItem) || playSessionStateProvider.isInErrorState()) {
                playCurrent();
            }
        } else if (playQueueItem.isAd() && playSessionStateProvider.isPlaying()) {
            playCurrent();
        }
        lastPlayQueueItem = playQueueItem;
    }


    private void onNextTrackWhileCasting(CurrentPlayQueueItemEvent event, PlayQueueItem playQueueItem) {
        if (event.isRepeat() || !lastPlayQueueItem.equals(playQueueItem)) {
            playCurrent();
        }
    }

    private boolean shouldPlayTrack(CurrentPlayQueueItemEvent event, PlayQueueItem playQueueItem) {
        return playSessionStateProvider.isPlaying()
                && (event.isRepeat() || !lastPlayQueueItem.equals(playQueueItem));
    }
}
