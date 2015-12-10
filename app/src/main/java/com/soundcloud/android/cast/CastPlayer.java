package com.soundcloud.android.cast;

import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_CAST;
import static com.soundcloud.android.playback.PlaybackUtils.correctInitialPositionLegacy;

import com.google.android.gms.cast.MediaStatus;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.Player;
import com.soundcloud.android.playback.Player.PlayerState;
import com.soundcloud.android.playback.Player.Reason;
import com.soundcloud.android.playback.Player.StateTransition;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ProgressReporter;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;

@Singleton
public class CastPlayer extends VideoCastConsumerImpl implements ProgressReporter.ProgressPuller {

    private final CastOperations castOperations;
    private final VideoCastManager castManager;
    private final ProgressReporter progressReporter;
    private final PlayQueueManager playQueueManager;
    private final EventBus eventBus;

    private Subscription playCurrentSubscription = RxUtils.invalidSubscription();

    @Inject
    public CastPlayer(CastOperations castOperations,
                      VideoCastManager castManager,
                      ProgressReporter progressReporter,
                      PlayQueueManager playQueueManager,
                      EventBus eventBus) {
        this.castOperations = castOperations;
        this.castManager = castManager;
        this.progressReporter = progressReporter;
        this.playQueueManager = playQueueManager;
        this.eventBus = eventBus;

        castManager.addVideoCastConsumer(this);
        progressReporter.setProgressPuller(this);
    }

    @Override
    public void onDisconnected() {
        reportStateChange(getStateTransition(PlayerState.IDLE, Reason.NONE)); // possibly show disconnect error here instead?
    }

    @Override
    public void onRemoteMediaPlayerStatusUpdated() {
        onMediaPlayerStatusUpdatedListener(castManager.getPlaybackStatus(), castManager.getIdleReason());
    }

    void onMediaPlayerStatusUpdatedListener(int playerState, int idleReason) {
        switch (playerState) {
            case MediaStatus.PLAYER_STATE_PLAYING:
                reportStateChange(getStateTransition(PlayerState.PLAYING, Reason.NONE));
                break;

            case MediaStatus.PLAYER_STATE_PAUSED:
                // we have to suppress pause events while we should be playing.
                // The receiver sends thes back often, as in when the track first loads, even if autoplay is true
                reportStateChange(getStateTransition(PlayerState.IDLE, Reason.NONE));
                break;

            case MediaStatus.PLAYER_STATE_BUFFERING:
                reportStateChange(getStateTransition(PlayerState.BUFFERING, Reason.NONE));
                break;

            case MediaStatus.PLAYER_STATE_IDLE:
                final Reason translatedIdleReason = getTranslatedIdleReason(idleReason);
                if (translatedIdleReason != null) {
                    reportStateChange(getStateTransition(PlayerState.IDLE, translatedIdleReason));
                }
                break;

            case MediaStatus.PLAYER_STATE_UNKNOWN:
                Log.e(this, "received an unknown media status"); // not sure when this happens yet
                break;
            default:
                throw new IllegalArgumentException("Unknown Media State code returned " + playerState);
        }
    }

    private void reportStateChange(StateTransition stateTransition) {
        eventBus.publish(EventQueue.PLAYBACK_STATE_CHANGED, stateTransition);
        final boolean playerPlaying = stateTransition.isPlayerPlaying();
        if (playerPlaying) {
            progressReporter.start();
        } else {
            progressReporter.stop();
        }
    }

    @Override
    public void pullProgress() {
        try {
            final PlaybackProgress playbackProgress = new PlaybackProgress(castManager.getCurrentMediaPosition(), castManager.getMediaDuration());
            eventBus.publish(EventQueue.PLAYBACK_PROGRESS, PlaybackProgressEvent.create(playbackProgress, castOperations.getRemoteCurrentTrackUrn()));
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(CastOperations.TAG, "Unable to report progress", e);
        }
    }

    private StateTransition getStateTransition(PlayerState state, Reason reason) {
        return new StateTransition(state, reason, castOperations.getRemoteCurrentTrackUrn(), getProgress(), getDuration());
    }

    @Nullable
    private Reason getTranslatedIdleReason(int idleReason) {
        switch (idleReason) {
            case MediaStatus.IDLE_REASON_ERROR:
                return Reason.ERROR_FAILED;
            case MediaStatus.IDLE_REASON_FINISHED:
                return Reason.PLAYBACK_COMPLETE;
            case MediaStatus.IDLE_REASON_CANCELED:
                return Reason.NONE;
            default:
                // do not fail fast, we want to ignore non-handled codes
                return null;
        }
    }

    Observable<PlaybackResult> reloadCurrentQueue() {
        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
        if (currentPlayQueueItem.isTrack()) {
            return setNewQueue(
                    getCurrentQueueUrnsWithoutAds(),
                    currentPlayQueueItem.getUrn(),
                    playQueueManager.getCurrentPlaySessionSource());
        } else {
            return Observable.just(PlaybackResult.error(TRACK_UNAVAILABLE_CAST));
        }
    }

    public Observable<PlaybackResult> setNewQueue(List<Urn> unfilteredLocalPlayQueueTracks,
                                                  final Urn initialTrackUrnCandidate,
                                                  final PlaySessionSource playSessionSource) {
        return castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(initialTrackUrnCandidate, unfilteredLocalPlayQueueTracks)
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(playNewLocalQueueOnRemote(initialTrackUrnCandidate, playSessionSource))
                .doOnError(reportPlaybackError(initialTrackUrnCandidate));
    }

    private Func1<LocalPlayQueue, Observable<PlaybackResult>> playNewLocalQueueOnRemote(final Urn initialTrackUrnCandidate,
                                                                                        final PlaySessionSource playSessionSource) {
        return new Func1<LocalPlayQueue, Observable<PlaybackResult>>() {
            @Override
            public Observable<PlaybackResult> call(LocalPlayQueue localPlayQueue) {
                if (localPlayQueue.isEmpty() || isInitialTrackDifferent(localPlayQueue)) {
                    return Observable.just(PlaybackResult.error(TRACK_UNAVAILABLE_CAST));
                } else {
                    reportStateChange(new StateTransition(PlayerState.BUFFERING, Reason.NONE, localPlayQueue.currentTrackUrn));
                    setNewPlayQueue(localPlayQueue, playSessionSource);
                    return Observable.just(PlaybackResult.success());
                }
            }

            private boolean isInitialTrackDifferent(LocalPlayQueue localPlayQueue) {
                return initialTrackUrnCandidate != Urn.NOT_SET &&
                        !initialTrackUrnCandidate.equals(localPlayQueue.currentTrackUrn);
            }
        };
    }

    public void playCurrent() {
        playCurrent(0L);
    }

    public void playCurrent(long position) {
        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
        final Urn currentTrackUrn = currentPlayQueueItem.getUrn();
        if (isCurrentlyLoadedOnRemotePlayer(currentTrackUrn)) {
            reconnectToExistingSession();
        } else {
            reportStateChange(new StateTransition(PlayerState.BUFFERING, Reason.NONE, currentTrackUrn));
            playCurrentSubscription.unsubscribe();
            playCurrentSubscription = castOperations.loadLocalPlayQueue(currentTrackUrn, playQueueManager.getCurrentQueueTrackUrns())
                    .subscribe(new PlayCurrentLocalQueueOnRemote(currentTrackUrn, position));
        }
    }

    private class PlayCurrentLocalQueueOnRemote extends DefaultSubscriber<LocalPlayQueue> {
        private final Urn currentTrackUrn;
        private final long position;

        private PlayCurrentLocalQueueOnRemote(Urn currentTrackUrn, long position) {
            this.currentTrackUrn = currentTrackUrn;
            this.position = position;
        }

        @Override
        public void onNext(LocalPlayQueue localPlayQueue) {
            playLocalQueueOnRemote(localPlayQueue, position);
        }

        @Override
        public void onError(Throwable e) {
            reportStateChange(new Player.StateTransition(PlayerState.IDLE, Player.Reason.ERROR_FAILED, currentTrackUrn));
        }
    }

    private void setNewPlayQueue(LocalPlayQueue localPlayQueue, PlaySessionSource playSessionSource) {
        playQueueManager.setNewPlayQueue(
                PlayQueue.fromTrackUrnList(localPlayQueue.playQueueTrackUrns, playSessionSource, Collections.<Urn, Boolean>emptyMap()),
                playSessionSource, correctInitialPositionLegacy(localPlayQueue.playQueueTrackUrns, 0, localPlayQueue.currentTrackUrn)
        );
    }

    private void playLocalQueueOnRemote(LocalPlayQueue localPlayQueue, long progressPosition) {
        try {
            castManager.loadMedia(localPlayQueue.mediaInfo, true, (int) progressPosition, localPlayQueue.playQueueTracksJSON);
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(CastOperations.TAG, "Unable to load track", e);
        }
    }

    private Action1<Throwable> reportPlaybackError(final Urn initialTrackUrnCandidate) {
        return new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                reportStateChange(new StateTransition(PlayerState.IDLE, Reason.ERROR_FAILED, initialTrackUrnCandidate));
            }
        };
    }

    private List<Urn> getCurrentQueueUrnsWithoutAds() {
        return new PlayQueue(playQueueManager.filterAdQueueItems()).getTrackItemUrns();
    }

    private boolean isCurrentlyLoadedOnRemotePlayer(Urn urn) {
        final Urn currentPlayingUrn = castOperations.getRemoteCurrentTrackUrn();
        return currentPlayingUrn != Urn.NOT_SET && currentPlayingUrn.equals(urn);
    }

    private void reconnectToExistingSession() {
        onMediaPlayerStatusUpdatedListener(castManager.getPlaybackStatus(), castManager.getIdleReason());
    }

    public boolean resume() {
        try {
            castManager.play();
            return true;
        } catch (CastException | TransientNetworkDisconnectionException | NoConnectionException | IllegalStateException e) {
            Log.e(CastOperations.TAG, "Unable to resume playback", e);
        }
        return false;
    }

    public void pause() {
        try {
            castManager.pause();
        } catch (CastException | TransientNetworkDisconnectionException | NoConnectionException | IllegalStateException e) {
            Log.e(CastOperations.TAG, "Unable to pause playback", e);
        }
    }

    public void togglePlayback() {
        try {
            castManager.togglePlayback();
        } catch (CastException | TransientNetworkDisconnectionException | NoConnectionException | IllegalStateException e) {
            Log.e(CastOperations.TAG, "Unable to pause playback", e);
        }
    }

    public long seek(long ms) {
        try {
            castManager.seek((int) ms);
            progressReporter.stop();
        } catch (TransientNetworkDisconnectionException | NoConnectionException | IllegalStateException e) {
            Log.e(CastOperations.TAG, "Unable to seek", e);
        }
        return ms;
    }

    public long getProgress() {
        try {
            return castManager.getCurrentMediaPosition();
        } catch (TransientNetworkDisconnectionException | NoConnectionException | IllegalStateException e) {
            Log.e(CastOperations.TAG, "Unable to get progress", e);
        }
        return 0;
    }

    private long getDuration() {
        try {
            return castManager.getMediaDuration();
        } catch (TransientNetworkDisconnectionException | NoConnectionException | IllegalStateException e) {
            Log.e(CastOperations.TAG, "Unable to get duration", e);
        }
        return 0;
    }

    public void stop() {
        pause(); // stop has more long-running implications in cast. pause is sufficient
    }

    public void destroy() {
        castManager.onDeviceSelected(null);
        castManager.removeVideoCastConsumer(this);
    }
}
