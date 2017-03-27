package com.soundcloud.android.cast.legacy;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.soundcloud.android.Actions;
import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.utils.Log;
import com.soundcloud.rx.eventbus.EventBus;
import rx.functions.Action1;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Collections;

@Singleton
public class LegacyCastSessionController extends VideoCastConsumerImpl
        implements VideoCastManager.MediaRouteDialogListener {

    private final LegacyCastOperations castOperations;
    private final PlaybackServiceController serviceController;
    private final LegacyCastPlayer castPlayer;
    private final PlayQueueManager playQueueManager;
    private final VideoCastManager videoCastManager;
    private final EventBus eventBus;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriber;

    @Inject
    public LegacyCastSessionController(LegacyCastOperations castOperations,
                                       PlaybackServiceController serviceController,
                                       LegacyCastPlayer castPlayer,
                                       PlayQueueManager playQueueManager,
                                       VideoCastManager videoCastManager,
                                       EventBus eventBus,
                                       PlaySessionStateProvider playSessionStateProvider,
                                       Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider) {
        this.castOperations = castOperations;
        this.serviceController = serviceController;
        this.castPlayer = castPlayer;
        this.playQueueManager = playQueueManager;
        this.videoCastManager = videoCastManager;
        this.eventBus = eventBus;
        this.playSessionStateProvider = playSessionStateProvider;
        this.expandPlayerSubscriber = expandPlayerSubscriberProvider;
    }

    public void startListening() {
        videoCastManager.addVideoCastConsumer(this);
        videoCastManager.setMediaRouteDialogListener(this);
    }

    @Override
    public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId, boolean wasLaunched) {
        Log.d(LegacyCastOperations.TAG, "On Application Connected, launched: " + wasLaunched);
        serviceController.stopPlaybackService();
        if (wasLaunched && !playQueueManager.isQueueEmpty()) {
            playLocalPlayQueueOnRemote();
        }
    }

    public void reconnectSessionIfPossible() {
        videoCastManager.reconnectSessionIfPossible();
    }

    private void playLocalPlayQueueOnRemote() {
        Log.d(LegacyCastOperations.TAG, "Sending current track and queue to cast receiver");
        final PlayQueueItem currentPlayQueueItem = playQueueManager.getCurrentPlayQueueItem();
        final PlaybackProgress lastProgressForTrack = currentPlayQueueItem.isEmpty() ? PlaybackProgress.empty() :
                                                      playSessionStateProvider.getLastProgressForItem(
                                                              currentPlayQueueItem.getUrn());

        castPlayer.reloadCurrentQueue()
                  .doOnNext(playCurrent(lastProgressForTrack))
                  .subscribe(expandPlayerSubscriber.get());
    }

    @NonNull
    private Action1<PlaybackResult> playCurrent(final PlaybackProgress lastProgressForTrack) {
        return playbackResult -> LegacyCastSessionController.this.castPlayer.playCurrent(lastProgressForTrack.getPosition());
    }

    @Override
    public void onRemoteMediaPlayerStatusUpdated() {
        Log.d(LegacyCastOperations.TAG,
              "On Status updated, status: " + videoCastManager.getRemoteMediaPlayer()
                                                              .getMediaStatus()
                                                              .getPlayerState());
        super.onRemoteMediaPlayerStatusUpdated();
    }

    @Override
    public void onRemoteMediaPlayerMetadataUpdated() {
        Log.d(LegacyCastOperations.TAG, "On metadata updated.");
        final RemotePlayQueue remotePlayQueue = castOperations.loadRemotePlayQueue();
        if (!remotePlayQueue.isEmpty()) {
            updateLocalPlayQueueAndPlayState(remotePlayQueue);
        }
    }

    private void updateLocalPlayQueueAndPlayState(RemotePlayQueue remotePlayQueue) {
        final int remotePosition = remotePlayQueue.getCurrentPosition();

        Log.d(LegacyCastOperations.TAG, "Loading " + remotePlayQueue);
        if (remotePlayQueue.hasSameTracks(playQueueManager.getCurrentQueueTrackUrns())) {
            if (!isIdleWithInterrupted()) {
                Log.d(LegacyCastOperations.TAG, "Has the same tracklist, setting remotePosition");
                playQueueManager.setPosition(remotePosition, true);
                if (videoCastManager.getPlaybackStatus() == MediaStatus.PLAYER_STATE_PLAYING) {
                    castPlayer.playCurrent();
                }
            }
        } else {
            Log.d(LegacyCastOperations.TAG, "Does not have the same tracklist, updating locally");
            final PlayQueue playQueue = remotePlayQueue.toPlayQueue(PlaySessionSource.forCast(), Collections.emptyMap());
            playQueueManager.setNewPlayQueue(playQueue, PlaySessionSource.forCast(), remotePosition);
            castPlayer.playCurrent();
        }

        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.showPlayer());
    }

    private boolean isIdleWithInterrupted() {
        return videoCastManager.getPlaybackStatus() == MediaStatus.PLAYER_STATE_IDLE
                && videoCastManager.getIdleReason() == MediaStatus.IDLE_REASON_INTERRUPTED;
    }

    @Override
    public void onMediaRouteDialogCellClick(Context context) {
        openStreamAndExpandPlayer(context);
    }

    private void openStreamAndExpandPlayer(Context context) {
        Intent intent = new Intent(Actions.STREAM)
                .putExtra(SlidingPlayerController.EXTRA_EXPAND_PLAYER, true)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

}
