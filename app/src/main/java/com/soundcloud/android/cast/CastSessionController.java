package com.soundcloud.android.cast;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.PlayQueue;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.utils.Log;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;

@Singleton
public class CastSessionController extends VideoCastConsumerImpl {

    private final CastOperations castOperations;
    private final PlaybackOperations playbackOperations;
    private final PlayQueueManager playQueueManager;
    private final VideoCastManager videoCastManager;
    private final EventBus eventBus;
    private final PlaySessionStateProvider playSessionStateProvider;

    @Inject
    public CastSessionController(CastOperations castOperations,
                                 PlaybackOperations playbackOperations, PlayQueueManager playQueueManager,
                                 VideoCastManager videoCastManager, EventBus eventBus,
                                 PlaySessionStateProvider playSessionStateProvider) {
        this.castOperations = castOperations;
        this.playbackOperations = playbackOperations;
        this.playQueueManager = playQueueManager;
        this.videoCastManager = videoCastManager;
        this.eventBus = eventBus;
        this.playSessionStateProvider = playSessionStateProvider;
    }

    public void startListening() {
        videoCastManager.addVideoCastConsumer(this);
    }

    @Override
    public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId, boolean wasLaunched) {
        Log.d(CastOperations.TAG, "On Application Connected, launched: " + wasLaunched);
        playbackOperations.stopService();
        if (wasLaunched && !playQueueManager.isQueueEmpty()) {
            Log.d(CastOperations.TAG, "Sending current track to cast device");
            final PlaybackProgress lastProgressByUrn = playSessionStateProvider.getLastProgressByUrn(playQueueManager.getCurrentTrackUrn());
            playbackOperations.playCurrent(lastProgressByUrn.getPosition());
        }
    }

    @Override
    public void onRemoteMediaPlayerStatusUpdated() {
        Log.d(CastOperations.TAG, "On Status updated, status: " + videoCastManager.getRemoteMediaPlayer().getMediaStatus().getPlayerState());
        super.onRemoteMediaPlayerStatusUpdated();
    }

    @Override
    public void onRemoteMediaPlayerMetadataUpdated() {
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
                playbackOperations.playCurrent();
            }
        } else {
            Log.d(CastOperations.TAG, "Does not have the same tracklist, updating locally");
            final PlayQueue playQueue = PlayQueue.fromTrackUrnList(
                    remoteTrackList.isEmpty() ? Arrays.asList(remoteCurrentUrn) : remoteTrackList,
                    PlaySessionSource.EMPTY);
            playQueueManager.setNewPlayQueue(playQueue, remotePosition, PlaySessionSource.EMPTY);
            playbackOperations.playCurrent();
        }

        eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.showPlayer());
    }

}
