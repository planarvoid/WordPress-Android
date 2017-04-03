package com.soundcloud.android.cast;

import static com.soundcloud.android.cast.api.CastProtocol.TAG;
import static com.soundcloud.android.playback.PlaybackUtils.correctInitialPosition;

import com.soundcloud.android.cast.api.CastPlayQueue;
import com.soundcloud.android.cast.api.CastProtocol;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.utils.Log;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;

@Singleton
class DefaultCastPlayer implements CastPlayer, CastProtocol.Listener {

    private final PlayQueueManager playQueueManager;
    private final EventBus eventBus;
    private final CastProtocol castProtocol;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final CastQueueController castQueueController;
    private final CastPlayStateReporter playStateReporter;
    private final CastQueueSlicer queueSlicer;

    private boolean autoplayIfRemoteIsEmpty;

    @Inject
    DefaultCastPlayer(PlayQueueManager playQueueManager,
                      EventBus eventBus,
                      CastProtocol castProtocol,
                      PlaySessionStateProvider playSessionStateProvider,
                      CastQueueController castQueueController,
                      CastPlayStateReporter playStateReporter,
                      CastQueueSlicer castQueueSlicer) {
        this.playQueueManager = playQueueManager;
        this.eventBus = eventBus;
        this.castProtocol = castProtocol;
        this.playSessionStateProvider = playSessionStateProvider;
        this.castQueueController = castQueueController;
        this.playStateReporter = playStateReporter;
        this.queueSlicer = castQueueSlicer;
    }

    @Override
    public void onConnected(boolean wasPlaying) {
        autoplayIfRemoteIsEmpty = wasPlaying;
    }

    @Override
    public void onDisconnected() {
        // fetch progress/duration from the storage since we have no more access to RemoteMediaClient after disconnection
        PlaybackProgress progress = playSessionStateProvider.getLastProgressEvent();
        playStateReporter.reportDisconnection(progress.getUrn(), progress.getPosition(), progress.getDuration());
    }

    @Override
    public void onIdle(long progress, long duration, PlayStateReason idleReason) {
        playStateReporter.reportIdle(idleReason, castQueueController.getRemoteCurrentTrackUrn(), progress, duration);
    }

    @Override
    public void onPlaying(long progress, long duration) {
        playStateReporter.reportPlaying(castQueueController.getRemoteCurrentTrackUrn(), progress, duration);
    }

    @Override
    public void onBuffering(long progress, long duration) {
        playStateReporter.reportBuffering(castQueueController.getRemoteCurrentTrackUrn(), progress, duration);
    }

    @Override
    public void onPaused(long progress, long duration) {
        playStateReporter.reportPaused(castQueueController.getRemoteCurrentTrackUrn(), progress, duration);
    }

    @Override
    public void onProgressUpdated(long progressMs, long durationMs) {
        final Urn currentRemoteTrackUrn = castQueueController.getRemoteCurrentTrackUrn();
        PlaybackProgressEvent progressEvent = PlaybackProgressEvent.create(new PlaybackProgress(progressMs, durationMs, currentRemoteTrackUrn), currentRemoteTrackUrn);

        playSessionStateProvider.onProgressEvent(progressEvent);
    }

    @Override
    public void playCurrent() {
        boolean isRemoteEmpty = castQueueController.getCurrentQueue() == null || castQueueController.getCurrentQueue().isEmpty();

        if (isRemoteEmpty) {
            loadLocalOnRemote(true);
        } else {
            final Urn currentLocalTrackUrn = playQueueManager.getCurrentPlayQueueItem().getUrn();
            if (castQueueController.isCurrentlyLoadedOnRemotePlayer(currentLocalTrackUrn)) {
                castProtocol.requestStatusUpdate();
            } else {
                updateRemoteQueue(currentLocalTrackUrn);
            }
        }
    }

    private void loadLocalOnRemote(boolean autoplay) {
        final Urn currentTrackUrn = playQueueManager.getCurrentPlayQueueItem().getUrn();
        long currentTrackPosition = playSessionStateProvider.getLastProgressForItem(currentTrackUrn).getPosition();
        List<Urn> slicedUrnList = queueSlicer.slice(playQueueManager.getCurrentQueueTrackUrns(), playQueueManager.getCurrentQueueTrackUrns().indexOf(currentTrackUrn)).getTrackItemUrns();
        CastPlayQueue castPlayQueue = castQueueController.buildCastPlayQueue(currentTrackUrn, slicedUrnList);
        castProtocol.sendLoad(currentTrackUrn.toString(), autoplay, currentTrackPosition, castPlayQueue);
    }

    private void updateRemoteQueue(Urn currentLocalTrackUrn) {
        final CastPlayQueue castPlayQueue;
        if (castQueueController.getCurrentQueue().contains(currentLocalTrackUrn)) {
            castPlayQueue = castQueueController.buildUpdatedCastPlayQueue(currentLocalTrackUrn, PlaySessionController.SEEK_POSITION_RESET);
            Log.d(TAG, "updateRemoteQueue() called with: newRemoteIndex = [" + castQueueController.getCurrentQueue().currentIndex() + " -> " + castPlayQueue.currentIndex() + "]");
        } else {
            resume();
            castPlayQueue = castQueueController.buildCastPlayQueue(currentLocalTrackUrn, playQueueManager.getCurrentQueueTrackUrns());
            Log.d(TAG, "updateRemoteQueue() called with: new track list for current urn = [" + currentLocalTrackUrn + "]");
        }
        castProtocol.sendUpdateQueue(castPlayQueue);
    }

    @Override
    public Observable<PlaybackResult> setNewQueue(final PlayQueue playQueue,
                                                  final Urn initialTrackUrn,
                                                  final PlaySessionSource playSessionSource) {
        return Observable.fromCallable(() -> {
            playStateReporter.reportPlayingReset(initialTrackUrn);
            PlayQueue slicedPlayQueue = queueSlicer.slice(playQueue.getTrackItemUrns(), playQueue.getTrackItemUrns().indexOf(initialTrackUrn));
            playQueueManager.setNewPlayQueue(slicedPlayQueue, playSessionSource, correctInitialPosition(slicedPlayQueue, 0, initialTrackUrn));
            return PlaybackResult.success();
        }).subscribeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void onRemoteEmptyStateFetched() {
        if (!isLocalEmpty()) {
            loadLocalOnRemote(autoplayIfRemoteIsEmpty);
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
        final int remotePosition = remoteQueue.currentIndex();

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
    public void resume() {
        castProtocol.play();
    }

    @Override
    public void pause() {
        castProtocol.pause();
    }

    @Override
    public void togglePlayback() {
        castProtocol.togglePlayback();
    }

    @Override
    public void seek(long position) {
        castProtocol.seek(position, this::onProgressUpdated);
    }
}
