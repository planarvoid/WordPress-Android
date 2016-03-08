package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.UNSKIPPABLE;
import static com.soundcloud.android.playback.Player.PlayerState;
import static com.soundcloud.android.playback.Player.StateTransition;

import com.soundcloud.android.PlaybackServiceInitiator;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.settings.SettingKey;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

import android.content.SharedPreferences;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class PlaySessionController {

    private static final String TAG = "PlaySessionController";

    @VisibleForTesting
    static final int RECOMMENDED_LOAD_TOLERANCE = 5;

    private static final long PROGRESS_THRESHOLD_FOR_TRACK_CHANGE = TimeUnit.SECONDS.toMillis(3L);
    private static final long SEEK_POSITION_RESET = 0L;

    private final EventBus eventBus;
    private final AdsOperations adsOperations;

    private final AdsController adsController;
    private final PlayQueueOperations playQueueOperations;
    private final PlayQueueManager playQueueManager;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final CastConnectionHelper castConnectionHelper;
    private final SharedPreferences sharedPreferences;
    private final NetworkConnectionHelper connectionHelper;
    private final Provider<PlaybackStrategy> playbackStrategyProvider;
    private final PlaybackToastHelper playbackToastHelper;
    private final AccountOperations accountOperations;
    private final StationsOperations stationsOperations;
    private final PlaybackServiceInitiator playbackServiceInitiator;

    private final Action1<PlayQueue> appendPlayQueueItems = new Action1<PlayQueue>() {
        @Override
        public void call(PlayQueue playQueue) {
            playQueueManager.appendPlayQueueItems(playQueue);
        }
    };

    private Subscription currentTrackSubscription = RxUtils.invalidSubscription();
    private Subscription loadRecommendedSubscription = RxUtils.invalidSubscription();
    private Subscription subscription = RxUtils.invalidSubscription();

    private boolean stopContinuousPlayback; // killswitch. If the api returns no tracks, stop asking for them

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

    private final Action1<PlayQueue> showPlayerAsCollapsed = new Action1<PlayQueue>() {
        @Override
        public void call(PlayQueue ignore) {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.collapsePlayer());
        }
    };

    private final Func1<StateTransition, Boolean> shouldAdvanceTracks = new Func1<StateTransition, Boolean>() {
        @Override
        public Boolean call(StateTransition stateTransition) {
            return playQueueManager.isCurrentTrack(stateTransition.getUrn())
                    && stateTransition.isPlayerIdle()
                    && !stateTransition.isPlayQueueComplete()
                    && (stateTransition.playbackEnded() || unrecoverableErrorDuringAutoplay(stateTransition));
        }
    };

    private final Action1<StateTransition> reconfigureUpcomingAd = new Action1<StateTransition>() {
        @Override
        public void call(StateTransition stateTransition) {
            adsController.reconfigureAdForNextTrack();
            adsController.publishAdDeliveryEventIfUpcoming();
        }
    };

    @Inject
    public PlaySessionController(EventBus eventBus,
                                 AdsOperations adsOperations,
                                 AdsController adsController,
                                 PlayQueueManager playQueueManager,
                                 PlayQueueOperations playQueueOperations,
                                 PlaySessionStateProvider playSessionStateProvider,
                                 CastConnectionHelper castConnectionHelper,
                                 SharedPreferences sharedPreferences,
                                 NetworkConnectionHelper connectionHelper,
                                 Provider<PlaybackStrategy> playbackStrategyProvider,
                                 PlaybackToastHelper playbackToastHelper,
                                 AccountOperations accountOperations,
                                 StationsOperations stationsOperations,
                                 PlaybackServiceInitiator playbackServiceInitiator) {
        this.eventBus = eventBus;
        this.adsOperations = adsOperations;
        this.adsController = adsController;
        this.playQueueManager = playQueueManager;
        this.playQueueOperations = playQueueOperations;
        this.sharedPreferences = sharedPreferences;
        this.connectionHelper = connectionHelper;
        this.playbackStrategyProvider = playbackStrategyProvider;
        this.playbackToastHelper = playbackToastHelper;
        this.accountOperations = accountOperations;
        this.stationsOperations = stationsOperations;
        this.playbackServiceInitiator = playbackServiceInitiator;
        this.playSessionStateProvider = playSessionStateProvider;
        this.castConnectionHelper = castConnectionHelper;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, new PlayQueueTrackSubscriber());
        eventBus.subscribe(EventQueue.PLAY_QUEUE, new PlayQueueSubscriber());
        eventBus.queue(EventQueue.PLAYBACK_STATE_CHANGED)
                .filter(shouldAdvanceTracks)
                .doOnNext(reconfigureUpcomingAd)
                .subscribe(new AdvanceTrackSubscriber());
    }

    public void reloadQueueAndShowPlayerIfEmpty() {
        if (playQueueManager.isQueueEmpty()) {
            subscription.unsubscribe();
            subscription = playQueueManager.loadPlayQueueAsync()
                    .doOnNext(showPlayerAsCollapsed)
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
                publishSkipEventIfAudioAd();
                playQueueManager.moveToPreviousPlayableItem();
            }
        }
    }

    public void nextTrack() {
        if (shouldDisableSkipping()) {
            playbackToastHelper.showUnskippableAdToast();
        } else {
            publishSkipEventIfAudioAd();
            playQueueManager.moveToNextPlayableItem();
        }
    }

    public boolean shouldDisableSkipping() {
        return adsOperations.isCurrentItemAd() &&
                playSessionStateProvider.getLastProgressEventForCurrentPlayQueueItem().getPosition() < AdConstants.UNSKIPPABLE_TIME_MS;
    }

    public void setCurrentPlayQueueItem(PlayQueueItem playQueueItem) {
        if (!playQueueManager.getCurrentPlayQueueItem().equals(playQueueItem)) {
            adsController.publishAdDeliveryEventIfUpcoming();
            publishSkipEventIfAudioAd();
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

    private void publishSkipEventIfAudioAd() {
        if (adsOperations.isCurrentItemAudioAd()) {
            final TrackQueueItem trackQueueItem = (TrackQueueItem) playQueueManager.getCurrentPlayQueueItem();
            final AudioAd audioAd = (AudioAd) trackQueueItem.getAdData().get();
            final UIEvent event = UIEvent.fromSkipAudioAdClick(audioAd, trackQueueItem.getUrn(),
                    accountOperations.getLoggedInUserUrn(), playQueueManager.getCurrentTrackSourceInfo());
            eventBus.publish(EventQueue.TRACKING, event);
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

    private class AdvanceTrackSubscriber extends DefaultSubscriber<StateTransition> {
        @Override
        public void onNext(StateTransition stateTransition) {
            if (!playQueueManager.autoMoveToNextPlayableItem()) {
                eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, createPlayQueueCompleteEvent(stateTransition.getUrn()));
            } else if (!stateTransition.playSessionIsActive()) {
                playCurrent();
            }
        }
    }

    private boolean unrecoverableErrorDuringAutoplay(StateTransition stateTransition) {
        final TrackSourceInfo currentTrackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        return stateTransition.wasError() && !stateTransition.wasGeneralFailure() &&
                currentTrackSourceInfo != null && !currentTrackSourceInfo.getIsUserTriggered()
                && connectionHelper.isNetworkConnected();
    }

    private class PlayQueueSubscriber extends DefaultSubscriber<PlayQueueEvent> {
        @Override
        public void onNext(PlayQueueEvent event) {
            if (event.isNewQueue()) {
                loadRecommendedSubscription.unsubscribe();
                stopContinuousPlayback = false;
            }
        }
    }

    private class PlayQueueTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {
        @Override
        public void onNext(CurrentPlayQueueItemEvent event) {
            if (withinRecommendedFetchTolerance() && isNotAlreadyLoadingRecommendations()) {
                final PlayQueueItem lastPlayQueueItem = playQueueManager.getLastPlayQueueItem();
                if (currentQueueAllowsRecommendations() && lastPlayQueueItem.isTrack()) {
                    loadRecommendedSubscription = playQueueOperations
                            .relatedTracksPlayQueue(lastPlayQueueItem.getUrn(), fromContinuousPlay())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnNext(appendPlayQueueItems)
                            .subscribe(new UpcomingTracksSubscriber());
                } else if (event.getCollectionUrn().isStation()) {
                    loadRecommendedSubscription = stationsOperations
                            .fetchUpcomingTracks(event.getCollectionUrn(), playQueueManager.getQueueSize())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnNext(appendPlayQueueItems)
                            .subscribe(new UpcomingTracksSubscriber());
                }
            }

            currentTrackSubscription.unsubscribe();

            final PlayQueueItem playQueueItem = event.getCurrentPlayQueueItem();
            if (playQueueItem.isTrack()) {
                if (castConnectionHelper.isCasting()) {
                    if (playSessionStateProvider.isPlaying() && !playQueueManager.isCurrentTrack(playQueueItem.getUrn())) {
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
        }
    }

    private boolean currentQueueAllowsRecommendations() {
        if (stopContinuousPlayback) {
            return false;
        } else {
            final PlaySessionSource currentPlaySessionSource = playQueueManager.getCurrentPlaySessionSource();
            final boolean isStation = playQueueManager.getCollectionUrn().isStation();
            return !isStation && (sharedPreferences.getBoolean(SettingKey.AUTOPLAY_RELATED_ENABLED, true) ||
                    currentPlaySessionSource.originatedInExplore() ||
                    Screen.DEEPLINK.get().equals(currentPlaySessionSource.getOriginScreen()));
        }
    }

    private boolean withinRecommendedFetchTolerance() {
        return !playQueueManager.isQueueEmpty() &&
                playQueueManager.getPlayableQueueItemsRemaining() <= RECOMMENDED_LOAD_TOLERANCE;
    }

    private boolean isNotAlreadyLoadingRecommendations() {
        return loadRecommendedSubscription.isUnsubscribed();
    }

    // Hacky, but the similar sounds service needs to know if it is allowed to not fulfill this request. This should
    // only be allowed if we are not in explore, or serving a deeplink. This should be removed after rollout and we
    // have determined the service can handle the load we give it...
    private boolean fromContinuousPlay() {
        final PlaySessionSource currentPlaySessionSource = playQueueManager.getCurrentPlaySessionSource();
        return !(currentPlaySessionSource.originatedInExplore() ||
                currentPlaySessionSource.originatedFromDeeplink() ||
                currentPlaySessionSource.originatedInSearchSuggestions());
    }

    private StateTransition createPlayQueueCompleteEvent(Urn trackUrn) {
        return new StateTransition(PlayerState.IDLE, Player.Reason.PLAY_QUEUE_COMPLETE, trackUrn);
    }

    private class UpcomingTracksSubscriber extends DefaultSubscriber<PlayQueue> {
        @Override
        public void onNext(PlayQueue playQueue) {
            stopContinuousPlayback = playQueue.isEmpty();
        }

        @Override
        public void onError(Throwable e) {
            if (e instanceof UnsupportedOperationException) {
                // we should not need this, as we should never get this far with an empty queue.
                // Just being defensive while we investigate
                // https://github.com/soundcloud/SoundCloud-Android/issues/3938

                final HashMap<String, String> valuePairs = new HashMap<>(2);
                valuePairs.put("Queue Size", String.valueOf(playQueueManager.getQueueSize()));
                valuePairs.put("PlaySessionSource", playQueueManager.getCurrentPlaySessionSource().toString());
                ErrorUtils.handleSilentException(e, valuePairs);
            } else {
                super.onError(e);
            }
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
