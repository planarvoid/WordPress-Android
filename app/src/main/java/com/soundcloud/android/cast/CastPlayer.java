package com.soundcloud.android.cast;

import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_CAST;
import static com.soundcloud.android.playback.PlaybackUtils.correctInitialPosition;

import com.google.android.gms.cast.MediaStatus;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ProgressReporter;
import com.soundcloud.android.playback.service.PlayQueue;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.playback.service.Playa.PlayaState;
import com.soundcloud.android.playback.service.Playa.Reason;
import com.soundcloud.android.playback.service.Playa.StateTransition;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class CastPlayer extends VideoCastConsumerImpl implements ProgressReporter.ProgressPusher {

    private final CastOperations castOperations;
    private final VideoCastManager castManager;
    private final ProgressReporter progressReporter;
    private final PlayQueueManager playQueueManager;
    private final AdsOperations adsOperations;
    private final EventBus eventBus;

    private Subscription playCurrentSubscription = Subscriptions.empty();

    @Inject
    public CastPlayer(CastOperations castOperations,
                      VideoCastManager castManager,
                      ProgressReporter progressReporter,
                      PlayQueueManager playQueueManager,
                      AdsOperations adsOperations,
                      EventBus eventBus) {
        this.castOperations = castOperations;
        this.castManager = castManager;
        this.progressReporter = progressReporter;
        this.playQueueManager = playQueueManager;
        this.adsOperations = adsOperations;
        this.eventBus = eventBus;

        castManager.addVideoCastConsumer(this);
        progressReporter.setProgressPusher(this);
    }

    @Override
    public void onDisconnected() {
        reportStateChange(getStateTransition(PlayaState.IDLE, Reason.NONE)); // possibly show disconnect error here instead?
    }

    @Override
    public void onRemoteMediaPlayerStatusUpdated() {
        onMediaPlayerStatusUpdatedListener(castManager.getPlaybackStatus(), castManager.getIdleReason());
    }

    void onMediaPlayerStatusUpdatedListener(int playerState, int idleReason) {
        switch (playerState) {
            case MediaStatus.PLAYER_STATE_PLAYING:
                reportStateChange(getStateTransition(PlayaState.PLAYING, Reason.NONE));
                break;

            case MediaStatus.PLAYER_STATE_PAUSED:
                // we have to suppress pause events while we should be playing.
                // The receiver sends thes back often, as in when the track first loads, even if autoplay is true
                reportStateChange(getStateTransition(PlayaState.IDLE, Reason.NONE));
                break;

            case MediaStatus.PLAYER_STATE_BUFFERING:
                reportStateChange(getStateTransition(PlayaState.BUFFERING, Reason.NONE));
                break;

            case MediaStatus.PLAYER_STATE_IDLE:
                final Reason translatedIdleReason = getTranslatedIdleReason(idleReason);
                if (translatedIdleReason != null) {
                    reportStateChange(getStateTransition(PlayaState.IDLE, translatedIdleReason));
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
    public void pushProgress() {
        try {
            final PlaybackProgress playbackProgress = new PlaybackProgress(castManager.getCurrentMediaPosition(), castManager.getMediaDuration());
            eventBus.publish(EventQueue.PLAYBACK_PROGRESS, new PlaybackProgressEvent(playbackProgress, castOperations.getRemoteCurrentTrackUrn()));
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(CastOperations.TAG, "Unable to report progress", e);
        }
    }

    private StateTransition getStateTransition(PlayaState state, Reason reason) {
        return new StateTransition(state, reason, castOperations.getRemoteCurrentTrackUrn(), getProgress(), getDuration());
    }

    @Nullable
    private Reason getTranslatedIdleReason(int idleReason) {
        switch (idleReason) {
            case MediaStatus.IDLE_REASON_ERROR:
                return Reason.ERROR_FAILED;
            case MediaStatus.IDLE_REASON_FINISHED:
                return Reason.TRACK_COMPLETE;
            case MediaStatus.IDLE_REASON_CANCELED:
                return Reason.NONE;
            default:
                // do not fail fast, we want to ignore non-handled codes
                return null;
        }
    }

    Observable<PlaybackResult> reloadAndPlayCurrentQueue(long withProgressPosition) {
        return playNewQueue(
                getCurrentQueueUrnsWithoutAds(),
                playQueueManager.getCurrentTrackUrn(),
                withProgressPosition,
                playQueueManager.getCurrentPlaySessionSource());
    }

    public Observable<PlaybackResult> playNewQueue(List<Urn> unfilteredLocalPlayQueueTracks,
                                                   final Urn initialTrackUrnCandidate,
                                                   final long withProgressPosition,
                                                   final PlaySessionSource playSessionSource) {
        return castOperations.loadLocalPlayQueueWithoutMonetizableAndPrivateTracks(initialTrackUrnCandidate, unfilteredLocalPlayQueueTracks)
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(playNewLocalQueueOnRemote(initialTrackUrnCandidate, withProgressPosition, playSessionSource))
                .doOnError(reportPlaybackError(initialTrackUrnCandidate));
    }

    private Func1<LocalPlayQueue, Observable<PlaybackResult>> playNewLocalQueueOnRemote(final Urn initialTrackUrnCandidate,
                                                                                        final long withProgressPosition,
                                                                                        final PlaySessionSource playSessionSource) {
        return new Func1<LocalPlayQueue, Observable<PlaybackResult>>() {
            @Override
            public Observable<PlaybackResult> call(LocalPlayQueue localPlayQueue) {
                if (localPlayQueue.isEmpty() || isInitialTrackDifferent(localPlayQueue)) {
                    return Observable.just(PlaybackResult.error(TRACK_UNAVAILABLE_CAST));
                } else {
                    reportStateChange(new StateTransition(PlayaState.BUFFERING, Reason.NONE, localPlayQueue.currentTrackUrn));
                    setNewPlayQueue(localPlayQueue, playSessionSource);
                    playLocalQueueOnRemote(localPlayQueue, withProgressPosition);
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
        Urn currentTrackUrn = playQueueManager.getCurrentTrackUrn();
        if (isCurrentlyLoadedOnRemotePlayer(currentTrackUrn)) {
            reconnectToExistingSession();
        } else {
            reportStateChange(new StateTransition(PlayaState.BUFFERING, Reason.NONE, currentTrackUrn));
            playCurrentSubscription.unsubscribe();
            playCurrentSubscription = castOperations.loadLocalPlayQueue(currentTrackUrn, playQueueManager.getCurrentQueueAsUrnList())
                    .subscribe(new PlayCurrentLocalQueueOnRemote(currentTrackUrn));
        }
    }

    private class PlayCurrentLocalQueueOnRemote extends DefaultSubscriber<LocalPlayQueue> {
        private final Urn currentTrackUrn;
        private PlayCurrentLocalQueueOnRemote(Urn currentTrackUrn) {
            this.currentTrackUrn = currentTrackUrn;
        }

        @Override
        public void onNext(LocalPlayQueue localPlayQueue) {
            playLocalQueueOnRemote(localPlayQueue, 0L);
        }

        @Override
        public void onError(Throwable e) {
            reportStateChange(new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_FAILED, currentTrackUrn));
        }
    }

    private void setNewPlayQueue(LocalPlayQueue localPlayQueue, PlaySessionSource playSessionSource) {
        playQueueManager.setNewPlayQueue(
                PlayQueue.fromTrackUrnList(localPlayQueue.playQueueTrackUrns, playSessionSource),
                correctInitialPosition(localPlayQueue.playQueueTrackUrns, 0, localPlayQueue.currentTrackUrn),
                playSessionSource);
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
                reportStateChange(new StateTransition(PlayaState.IDLE, Reason.ERROR_FAILED, initialTrackUrnCandidate));
            }
        };
    }

    private List<Urn> getCurrentQueueUrnsWithoutAds() {
        List<Urn> queueWithoutAds = playQueueManager.getCurrentQueueAsUrnList();
        queueWithoutAds.removeAll(adsOperations.getAdUrnsInQueue());
        return queueWithoutAds;
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
