package com.soundcloud.android.cast;

import static com.soundcloud.android.cast.CastProtocol.TAG;
import static com.soundcloud.android.playback.PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_CAST;

import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.Log;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;

@Singleton
class DefaultCastPlayer implements CastPlayer, CastProtocol.Listener {

    private final DefaultCastOperations castOperations;
    private final PlayQueueManager playQueueManager;
    private final EventBus eventBus;
    private final CastProtocol castProtocol;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final CastQueueController castQueueController;
    private final CastPlayStateReporter playStateReporter;

    private Subscription playCurrentSubscription = RxUtils.invalidSubscription();

    @Inject
    DefaultCastPlayer(DefaultCastOperations castOperations,
                      PlayQueueManager playQueueManager,
                      EventBus eventBus,
                      CastProtocol castProtocol,
                      PlaySessionStateProvider playSessionStateProvider,
                      CastQueueController castQueueController,
                      CastPlayStateReporter playStateReporter) {
        this.castOperations = castOperations;
        this.playQueueManager = playQueueManager;
        this.eventBus = eventBus;
        this.castProtocol = castProtocol;
        this.playSessionStateProvider = playSessionStateProvider;
        this.castQueueController = castQueueController;
        this.playStateReporter = playStateReporter;
    }

    @Override
    public void onConnected() {
        castProtocol.attachCredentials(castOperations.getCastCredentials());
    }

    @Override
    public void onDisconnected() {
        playStateReporter.reportDisconnection(castQueueController.getRemoteCurrentTrackUrn(), getProgress(), getDuration());
    }

    @Override
    public void onStatusUpdated() {
        reportPlayerState();
    }

    @Override
    public void onProgressUpdated(long progressMs, long durationMs) {
        final Urn currentRemoteTrackUrn = castQueueController.getRemoteCurrentTrackUrn();
        eventBus.publish(EventQueue.PLAYBACK_PROGRESS, PlaybackProgressEvent.create(new PlaybackProgress(progressMs, durationMs, currentRemoteTrackUrn), currentRemoteTrackUrn));
    }

    @VisibleForTesting
    void reportPlayerState() {
        final int remotePlayerState = getRemoteMediaClient().getPlayerState();
        final Urn remoteTrackUrn = castQueueController.getRemoteCurrentTrackUrn();
        final long remoteTrackProgress = getProgress();
        final long remoteTrackDuration = getDuration();

        switch (remotePlayerState) {
            case MediaStatus.PLAYER_STATE_PLAYING:
                playStateReporter.reportPlaying(remoteTrackUrn, remoteTrackProgress, remoteTrackDuration);
                break;
            case MediaStatus.PLAYER_STATE_PAUSED:
                playStateReporter.reportPaused(remoteTrackUrn, remoteTrackProgress, remoteTrackDuration);
                break;
            case MediaStatus.PLAYER_STATE_BUFFERING:
                playStateReporter.reportBuffering(remoteTrackUrn, remoteTrackProgress, remoteTrackDuration);
                break;
            case MediaStatus.PLAYER_STATE_IDLE:
                final PlayStateReason translatedIdleReason = getTranslatedIdleReason(getRemoteMediaClient().getIdleReason());
                if (translatedIdleReason != null) {
                    playStateReporter.reportIdle(translatedIdleReason, remoteTrackUrn, remoteTrackProgress, remoteTrackDuration);
                }
                break;
            case MediaStatus.PLAYER_STATE_UNKNOWN:
                Log.e(this, "received an unknown media status"); // not sure when this happens yet
                break;
            default:
                throw new IllegalArgumentException("Unknown Media State code returned " + remotePlayerState);
        }
    }

    @Nullable
    private PlayStateReason getTranslatedIdleReason(int idleReason) {
        switch (idleReason) {
            case MediaStatus.IDLE_REASON_ERROR:
                return PlayStateReason.ERROR_FAILED;
            case MediaStatus.IDLE_REASON_FINISHED:
                return PlayStateReason.PLAYBACK_COMPLETE;
            case MediaStatus.IDLE_REASON_CANCELED:
                return PlayStateReason.NONE;
            case MediaStatus.IDLE_REASON_INTERRUPTED:
            case MediaStatus.IDLE_REASON_NONE:
            default:
                // do not fail fast, we want to ignore non-handled codes
                return null;
        }
    }

    @Override
    public void playCurrent() {
        boolean isRemoteEmpty = castQueueController.getCurrentQueue() == null || castQueueController.getCurrentQueue().isEmpty();

        if (isRemoteEmpty) {
            loadLocalOnRemote();
        } else {
            final Urn currentLocalTrackUrn = playQueueManager.getCurrentPlayQueueItem().getUrn();
            if (castQueueController.isCurrentlyLoadedOnRemotePlayer(currentLocalTrackUrn)) {
                reportPlayerState();
            } else {
                updateRemoteQueue(currentLocalTrackUrn);
            }
        }
    }

    private void loadLocalOnRemote() {
        final Urn currentTrackUrn = playQueueManager.getCurrentPlayQueueItem().getUrn();
        PlaybackProgress lastProgress = playSessionStateProvider.getLastProgressEvent();

        playStateReporter.reportIdle(PlayStateReason.NONE, currentTrackUrn, lastProgress.getPosition(), lastProgress.getDuration());
        playCurrentSubscription.unsubscribe();

        playCurrentSubscription = castOperations
                .createLoadMessageParameters(currentTrackUrn, true, lastProgress.getPosition(), playQueueManager.getCurrentQueueTrackUrns())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new PlayCurrentLocalQueueOnRemote(currentTrackUrn));
    }

    private void updateRemoteQueue(Urn currentLocalTrackUrn) {
        final CastPlayQueue castPlayQueue;
        if (castQueueController.getCurrentQueue().contains(currentLocalTrackUrn)) {
            castPlayQueue = castQueueController.buildUpdatedCastPlayQueue(currentLocalTrackUrn);
            Log.d(TAG, "updateRemoteQueue() called with: newRemoteIndex = [" + castQueueController.getCurrentQueue().getCurrentIndex() + " -> " + castPlayQueue.getCurrentIndex() + "]");
        } else {
            castPlayQueue = castQueueController.buildCastPlayQueue(currentLocalTrackUrn, playQueueManager.getCurrentQueueTrackUrns());
            Log.d(TAG, "updateRemoteQueue() called with: new track list for current urn = [" + currentLocalTrackUrn + "]");
        }
        castProtocol.sendUpdateQueue(castPlayQueue);
    }

    private class PlayCurrentLocalQueueOnRemote extends DefaultSubscriber<LoadMessageParameters> {
        private final Urn currentTrackUrn;

        private PlayCurrentLocalQueueOnRemote(Urn currentTrackUrn) {
            this.currentTrackUrn = currentTrackUrn;
        }

        @Override
        public void onNext(LoadMessageParameters loadMessageParameters) {
            castProtocol.sendLoad(currentTrackUrn.toString(), loadMessageParameters.autoplay, loadMessageParameters.playPosition, loadMessageParameters.jsonData);
        }

        @Override
        public void onError(Throwable e) {
            playStateReporter.reportPlayingError(currentTrackUrn);
        }
    }

    @Override
    public Observable<PlaybackResult> setNewQueue(List<Urn> trackItemUrns,
                                                  final Urn initialTrackUrn,
                                                  final PlaySessionSource playSessionSource) {
        return castOperations.validateTracksToPlay(initialTrackUrn, trackItemUrns)
                             .observeOn(AndroidSchedulers.mainThread())
                             .map(filteredTrackUrns -> {
                                 if (filteredTrackUrns.isEmpty()) {
                                     return PlaybackResult.error(TRACK_UNAVAILABLE_CAST);
                                 } else {
                                     playStateReporter.reportPlayingReset(initialTrackUrn);
                                     castOperations.setNewPlayQueue(filteredTrackUrns, initialTrackUrn, playSessionSource);
                                     return PlaybackResult.success();
                                 }
                             })
                             .doOnError(throwable -> playStateReporter.reportPlayingError(initialTrackUrn));
    }

    @Override
    public void onRemoteEmptyStateFetched() {
        if (!isLocalEmpty()) {
            loadLocalOnRemote();
        }
    }

    private boolean isLocalEmpty() {
        return playQueueManager.getCurrentPlayQueueItem() == null || playQueueManager.getCurrentPlayQueueItem().isEmpty();
    }

    @Override
    public void onQueueReceived(CastPlayQueue castPlayQueue) {
        castQueueController.storePlayQueue(castPlayQueue);

        updateLocalPlayQueueAndPlayState(castPlayQueue);
        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.showPlayer());
    }

    private void updateLocalPlayQueueAndPlayState(@NonNull CastPlayQueue remoteQueue) {
        final int remotePosition = remoteQueue.getCurrentIndex();

        Log.d(TAG, "DefaultCastPlayer::localQueue = " + playQueueManager.getCurrentQueueTrackUrns());
        if (remoteQueue.hasSameTracks(playQueueManager.getCurrentQueueTrackUrns())) {
            Log.d(TAG, "DefaultCastPlayer::Has the same tracklist, setting local position to " + remotePosition);
            playQueueManager.setPosition(remotePosition, true);
        } else {
            Log.d(TAG, "DefaultCastPlayer::Remote is not the same as local queue -> REPLACE local");
            final PlayQueue playQueue = castQueueController.buildPlayQueue(PlaySessionSource.forCast(), Collections.emptyMap());
            playQueueManager.setNewPlayQueue(playQueue, PlaySessionSource.forCast(), remotePosition);
        }
        playCurrent();
    }

    @Override
    public boolean resume() {
        getRemoteMediaClient().play();
        return true;
    }

    @Override
    public void pause() {
        getRemoteMediaClient().pause();
    }

    @Override
    public void togglePlayback() {
        getRemoteMediaClient().togglePlayback();
    }

    @Override
    public long seek(long ms) {
        getRemoteMediaClient().seek((int) ms);
        return ms;
    }

    public long getProgress() {
        if (castProtocol.isConnected() && getRemoteMediaClient() != null) {
            return getRemoteMediaClient().getApproximateStreamPosition();
        } else {
            return playSessionStateProvider.getLastProgressEvent().getPosition();
        }
    }

    private long getDuration() {
        if (castProtocol.isConnected() && getRemoteMediaClient() != null) {
            return getRemoteMediaClient().getStreamDuration();
        } else {
            return playSessionStateProvider.getLastProgressEvent().getDuration();
        }
    }

    public void stop() {
        pause(); // stop has more long-running implications in cast. pause is sufficient
    }

    @Nullable
    private RemoteMediaClient getRemoteMediaClient() {
        return castProtocol.getRemoteMediaClient();
    }
}
