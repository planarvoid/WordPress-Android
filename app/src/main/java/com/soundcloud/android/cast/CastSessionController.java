package com.soundcloud.android.cast;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.soundcloud.android.Actions;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlayQueue;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.utils.Log;

import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

@Singleton
public class CastSessionController extends VideoCastConsumerImpl implements VideoCastManager.MediaRouteDialogListener {

    private final CastOperations castOperations;
    private final PlaybackOperations playbackOperations;
    private final CastPlayer castPlayer;
    private final PlayQueueManager playQueueManager;
    private final VideoCastManager videoCastManager;
    private final EventBus eventBus;
    private final PlaySessionStateProvider playSessionStateProvider;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriber;

    @Inject
    public CastSessionController(CastOperations castOperations,
                                 PlaybackOperations playbackOperations,
                                 CastPlayer castPlayer,
                                 PlayQueueManager playQueueManager,
                                 VideoCastManager videoCastManager,
                                 EventBus eventBus,
                                 PlaySessionStateProvider playSessionStateProvider,
                                 Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider) {
        this.castOperations = castOperations;
        this.playbackOperations = playbackOperations;
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
        Log.d(CastOperations.TAG, "On Application Connected, launched: " + wasLaunched);
        playbackOperations.stopService();
        if (wasLaunched && !playQueueManager.isQueueEmpty()) {
            playLocalPlayQueueOnRemote();
        }
    }

    private void playLocalPlayQueueOnRemote() {
        Log.d(CastOperations.TAG, "Sending current track and queue to cast receiver");
        final long fromLastProgressPosition = playSessionStateProvider.getLastProgressByUrn(playQueueManager.getCurrentTrackUrn()).getPosition();
        castPlayer.reloadAndPlayCurrentQueue(fromLastProgressPosition).subscribe(expandPlayerSubscriber.get());
    }

    @Override
    public void onRemoteMediaPlayerStatusUpdated() {
        Log.d(CastOperations.TAG, "On Status updated, status: " + videoCastManager.getRemoteMediaPlayer().getMediaStatus().getPlayerState());
        super.onRemoteMediaPlayerStatusUpdated();
    }

    @Override
    public void onRemoteMediaPlayerMetadataUpdated() {
        Log.d(CastOperations.TAG, "On metadata updated.");
        final RemotePlayQueue remotePlayQueue = castOperations.loadRemotePlayQueue();
        if (!remotePlayQueue.getTrackList().isEmpty()) {
            updateLocalPlayQueueAndPlayState(remotePlayQueue);
        }
    }

    private void updateLocalPlayQueueAndPlayState(RemotePlayQueue remotePlayQueue) {
        final List<Urn> remoteTrackList = remotePlayQueue.getTrackList();
        final Urn remoteCurrentUrn = remotePlayQueue.getCurrentTrackUrn();
        final int remotePosition = remotePlayQueue.getCurrentPosition();

        Log.d(CastOperations.TAG, String.format("Loading Remote Queue, CurrentUrn: %s, RemoteTrackListSize: %d", remoteCurrentUrn, remoteTrackList.size()));
        if (playQueueManager.hasSameTrackList(remoteTrackList)) {
            Log.d(CastOperations.TAG, "Has the same tracklist, setting remotePosition");
            playQueueManager.setPosition(remotePosition);
            if (videoCastManager.getPlaybackStatus() == MediaStatus.PLAYER_STATE_PLAYING) {
                castPlayer.playCurrent();
            }
        } else {
            Log.d(CastOperations.TAG, "Does not have the same tracklist, updating locally");
            final PlayQueue playQueue = PlayQueue.fromTrackUrnList(
                    remoteTrackList.isEmpty() ? Arrays.asList(remoteCurrentUrn) : remoteTrackList,
                    PlaySessionSource.EMPTY);
            playQueueManager.setNewPlayQueue(playQueue, remotePosition, PlaySessionSource.EMPTY);
            castPlayer.playCurrent();
        }

        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.showPlayer());
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
