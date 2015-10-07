package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.UNSKIPPABLE;
import static com.soundcloud.android.playback.Player.PlayerState;
import static com.soundcloud.android.playback.Player.StateTransition;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.ads.AdConstants;
import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.settings.SettingKey;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.PropertySetFunctions;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Lazy;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Singleton
public class PlaySessionController {

    @VisibleForTesting
    static final int RECOMMENDED_LOAD_TOLERANCE = 5;
    public static final int SKIP_REPORT_TOLERANCE = 1000;

    private static final long PROGRESS_THRESHOLD_FOR_TRACK_CHANGE = TimeUnit.SECONDS.toMillis(3L);
    private static final long SEEK_POSITION_RESET = 0L;

    private final Resources resources;
    private final EventBus eventBus;
    private final AdsOperations adsOperations;
    private final AdsController adsController;
    private final PlayQueueOperations playQueueOperations;
    private final TrackRepository trackRepository;
    private final PlayQueueManager playQueueManager;
    private final IRemoteAudioManager audioManager;
    private final ImageOperations imageOperations;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final CastConnectionHelper castConnectionHelper;
    private final SharedPreferences sharedPreferences;
    private final NetworkConnectionHelper connectionHelper;
    private final Provider<PlaybackStrategy> playbackStrategyProvider;
    private final PlaybackToastHelper playbackToastHelper;
    private final AccountOperations accountOperations;
    private final StationsOperations stationsOperations;

    private final Func1<Bitmap, Bitmap> copyBitmap = new Func1<Bitmap, Bitmap>() {
        @Override
        public Bitmap call(Bitmap bitmap) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, false);
        }
    };

    private final Action1<PlayQueue> appendUniquePlayQueueItems = new Action1<PlayQueue>() {
        @Override
        public void call(PlayQueue playQueue) {
            playQueueManager.appendUniquePlayQueueItems(playQueue);
        }
    };

    private final Action1<PlayQueue> appendPlayQueueItems = new Action1<PlayQueue>() {
        @Override
        public void call(PlayQueue playQueue) {
            playQueueManager.appendPlayQueueItems(playQueue);
        }
    };

    private Subscription currentTrackSubscription = RxUtils.invalidSubscription();
    private Subscription loadRecommendedSubscription = RxUtils.invalidSubscription();

    private PropertySet currentPlayQueueTrack; // the track that is currently set in the queue
    private boolean stopContinuousPlayback; // killswitch. If the api returns no tracks, stop asking for them
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
    public PlaySessionController(Resources resources,
                                 EventBus eventBus,
                                 AdsOperations adsOperations,
                                 AdsController adsController,
                                 PlayQueueManager playQueueManager,
                                 TrackRepository trackRepository,
                                 Lazy<IRemoteAudioManager> audioManager,
                                 PlayQueueOperations playQueueOperations,
                                 ImageOperations imageOperations,
                                 PlaySessionStateProvider playSessionStateProvider,
                                 CastConnectionHelper castConnectionHelper,
                                 SharedPreferences sharedPreferences,
                                 NetworkConnectionHelper connectionHelper,
                                 Provider<PlaybackStrategy> playbackStrategyProvider,
                                 PlaybackToastHelper playbackToastHelper,
                                 AccountOperations accountOperations,
                                 StationsOperations stationsOperations) {
        this.resources = resources;
        this.eventBus = eventBus;
        this.adsOperations = adsOperations;
        this.adsController = adsController;
        this.playQueueManager = playQueueManager;
        this.trackRepository = trackRepository;
        this.playQueueOperations = playQueueOperations;
        this.sharedPreferences = sharedPreferences;
        this.connectionHelper = connectionHelper;
        this.playbackStrategyProvider = playbackStrategyProvider;
        this.playbackToastHelper = playbackToastHelper;
        this.accountOperations = accountOperations;
        this.stationsOperations = stationsOperations;
        this.audioManager = audioManager.get();
        this.imageOperations = imageOperations;
        this.playSessionStateProvider = playSessionStateProvider;
        this.castConnectionHelper = castConnectionHelper;
    }

    public void subscribe() {
        eventBus.subscribe(EventQueue.PLAYBACK_STATE_CHANGED, new PlayStateSubscriber());
        eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK, new PlayQueueTrackSubscriber());
        eventBus.subscribe(EventQueue.PLAY_QUEUE, new PlayQueueSubscriber());
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
        if (playSessionStateProvider.isPlayingCurrentPlayQueueTrack()) {
            playbackStrategyProvider.get().togglePlayback();
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
            if (playSessionStateProvider.isPlayingCurrentPlayQueueTrack()) {
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
                    && !adsOperations.isCurrentTrackAudioAd()) {
                seek(SEEK_POSITION_RESET);
            } else {
                publishSkipEventIfAudioAd();
                playQueueManager.moveToPreviousTrack();
            }
        }
    }

    public void nextTrack() {
        if (shouldDisableSkipping()) {
            playbackToastHelper.showUnskippableAdToast();
        } else {
            publishSkipEventIfAudioAd();
            playQueueManager.nextTrack();
        }
    }

    public boolean shouldDisableSkipping() {
        return adsOperations.isCurrentTrackAudioAd() &&
                playSessionStateProvider.getLastProgressEventForCurrentPlayQueueTrack().getPosition() < AdConstants.UNSKIPPABLE_TIME_MS;
    }

    public void setPlayQueuePosition(int position) {
        if (position != playQueueManager.getCurrentPosition()) {
            publishSkipEventIfAudioAd();
            playQueueManager.setPosition(position);
        }
    }

    public Observable<PlaybackResult> playNewQueue(PlayQueue playQueue, Urn initialTrack, int startPosition,
                                                   boolean loadRelated, PlaySessionSource playSessionSource) {
        if (shouldDisableSkipping()) {
            return Observable.just(PlaybackResult.error(UNSKIPPABLE));
        } else {
            return playbackStrategyProvider.get()
                    .setNewQueue(playQueue, initialTrack, startPosition, loadRelated, playSessionSource)
                    .doOnSubscribe(stopLoadingPreviousTrack)
                    .doOnNext(playCurrentTrack);
        }
    }

    private void publishSkipEventIfAudioAd() {
        if (adsOperations.isCurrentTrackAudioAd()) {
            final UIEvent event = UIEvent.fromSkipAudioAdClick(playQueueManager.getCurrentMetaData(), playQueueManager.getCurrentTrackUrn(),
                    accountOperations.getLoggedInUserUrn(), playQueueManager.getCurrentTrackSourceInfo());
            eventBus.publish(EventQueue.TRACKING, event);
        }
    }

    void playCurrent() {
        subscription.unsubscribe();
        Observable<Void> playCurrentObservable = playQueueManager.isQueueEmpty()
                ? playQueueManager.loadPlayQueueAsync().flatMap(toPlayCurrent)
                : playbackStrategyProvider.get().playCurrent();

        subscription = playCurrentObservable.subscribe(new DefaultSubscriber<Void>());
    }

    private class PlayStateSubscriber extends DefaultSubscriber<StateTransition> {
        @Override
        public void onNext(StateTransition stateTransition) {
            if (!StateTransition.DEFAULT.equals(stateTransition)) {
                audioManager.setPlaybackState(stateTransition.playSessionIsActive());
                skipOnTrackFinishOrUnplayable(stateTransition);
            }
        }
    }

    private void skipOnTrackFinishOrUnplayable(StateTransition stateTransition) {

        if (stateTransition.isPlayerIdle() && !stateTransition.isPlayQueueComplete()
                && (stateTransition.trackEnded() || unrecoverableErrorDuringAutoplay(stateTransition))) {
            logInvalidSkipping(stateTransition);

            adsController.reconfigureAdForNextTrack();

            tryToSkipTrack(stateTransition);
            if (!stateTransition.playSessionIsActive()) {
                playCurrent();
            }
        }
    }

    private void logInvalidSkipping(StateTransition stateTransition) {
        final PlaybackProgress progress = stateTransition.getProgress();
        if (stateTransition.trackEnded()) {
            if (Math.abs(progress.getDuration() - progress.getPosition()) > SKIP_REPORT_TOLERANCE) {
                ErrorUtils.handleSilentException(stateTransition.toString(), new IllegalStateException("Track ended prematurely"));
            }
        } else {
            if (progress.getPosition() > 0) {
                ErrorUtils.handleSilentException(stateTransition.toString(), new IllegalStateException("Skipping on track error too late"));
            }
        }
    }

    private boolean unrecoverableErrorDuringAutoplay(StateTransition stateTransition) {
        final TrackSourceInfo currentTrackSourceInfo = playQueueManager.getCurrentTrackSourceInfo();
        return stateTransition.wasError() && !stateTransition.wasGeneralFailure() &&
                currentTrackSourceInfo != null && !currentTrackSourceInfo.getIsUserTriggered()
                && connectionHelper.isNetworkConnected();
    }

    private void tryToSkipTrack(StateTransition stateTransition) {
        if (!playQueueManager.autoNextTrack()) {
            eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, createPlayQueueCompleteEvent(stateTransition.getTrackUrn()));
        }
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

    private class PlayQueueTrackSubscriber extends DefaultSubscriber<CurrentPlayQueueTrackEvent> {
        @Override
        public void onNext(CurrentPlayQueueTrackEvent event) {
            if (withinRecommendedFetchTolerance() && isNotAlreadyLoadingRecommendations()) {
                if (currentQueueAllowsRecommendations()) {
                    loadRecommendedSubscription = playQueueOperations
                            .relatedTracksPlayQueue(playQueueManager.getLastTrackUrn(), fromContinuousPlay())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnNext(appendUniquePlayQueueItems)
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
            currentTrackSubscription = trackRepository
                    .track(event.getCurrentTrackUrn())
                    .map(PropertySetFunctions.mergeInto(event.getCurrentMetaData()))
                    .subscribe(new CurrentTrackSubscriber());
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
                playQueueManager.getQueueSize() - playQueueManager.getCurrentPosition() <= RECOMMENDED_LOAD_TOLERANCE;
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

    private final class CurrentTrackSubscriber extends DefaultSubscriber<PropertySet> {
        @Override
        public void onNext(PropertySet track) {
            if (castConnectionHelper.isCasting()) {
                playIfTrackChanged(track);
            } else if (playSessionStateProvider.isPlaying()) {
                playCurrent();
            }

            currentPlayQueueTrack = track;
            updateRemoteAudioManager();
        }

        private void playIfTrackChanged(PropertySet newCurrentTrack) {
            Urn newCurrentTrackUrn = newCurrentTrack.get(TrackProperty.URN);
            Urn previousCurrentTrackUrn = getCurrentPlayQueueTrackUrn();
            if (playSessionStateProvider.isPlaying() &&
                    !newCurrentTrackUrn.equals(previousCurrentTrackUrn)) {
                playCurrent();
            }
        }
    }

    private Urn getCurrentPlayQueueTrackUrn() {
        return currentPlayQueueTrack == null ? Urn.NOT_SET : currentPlayQueueTrack.get(TrackProperty.URN);
    }

    private final class ArtworkSubscriber extends DefaultSubscriber<Bitmap> {
        @Override
        public void onNext(Bitmap bitmap) {
            try {
                audioManager.onTrackChanged(currentPlayQueueTrack, bitmap);
            } catch (IllegalArgumentException e) {
                logArtworkException(bitmap, e);
            }
        }

        private void logArtworkException(Bitmap bitmap, IllegalArgumentException e) {
            final String bitmapSize = bitmap == null ? "null" : bitmap.getWidth() + "x" + bitmap.getHeight();
            ErrorUtils.handleSilentException(e, Collections.singletonMap("bitmap_size", bitmapSize));
        }
    }

    private void updateRemoteAudioManager() {
        if (audioManager.isTrackChangeSupported()) {
            audioManager.onTrackChanged(currentPlayQueueTrack, null); // set initial data without bitmap so it doesn't have to wait
            final Urn resourceUrn = currentPlayQueueTrack.get(TrackProperty.URN);
            currentTrackSubscription = imageOperations.artwork(resourceUrn, ApiImageSize.getFullImageSize(resources))
                    .filter(validateBitmap(resourceUrn))
                    .map(copyBitmap)
                    .subscribe(new ArtworkSubscriber());
        }
    }

    // Trying to debug : https://github.com/soundcloud/SoundCloud-Android/issues/2984
    private Func1<Bitmap, Boolean> validateBitmap(final Urn resourceUrn) {
        return new Func1<Bitmap, Boolean>() {
            @Override
            public Boolean call(Bitmap bitmap) {
                if (bitmap == null) {
                    ErrorUtils.handleSilentException(new IllegalArgumentException("Artwork bitmap is null " + resourceUrn));
                    return false;
                } else if (bitmap.getWidth() <= 0 || bitmap.getHeight() <= 0) {
                    ErrorUtils.handleSilentException(new IllegalArgumentException("Artwork bitmap has no size " + resourceUrn));
                    return false;
                } else {
                    return true;
                }
            }
        };
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
}
