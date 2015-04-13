package com.soundcloud.android.cast;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Singleton
public class CastSessionController extends VideoCastConsumerImpl {

    private final String TAG = "CastSessionController";

    private final PlaybackOperations playbackOperations;
    private final PlayQueueManager playQueueManager;
    private final VideoCastManager videoCastManager;
    private final EventBus eventBus;
    private final PlaySessionStateProvider playSessionStateProvider;

    @Inject
    public CastSessionController(PlaybackOperations playbackOperations, PlayQueueManager playQueueManager,
                                 VideoCastManager videoCastManager, EventBus eventBus,
                                 PlaySessionStateProvider playSessionStateProvider) {

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
        Log.i(TAG, "On Application Connected, launched: " + wasLaunched);
        if (wasLaunched){
            if (playSessionStateProvider.isPlaying()){
                Log.i(TAG, "Sending current track to cast device");
                playbackOperations.stopService();
                final PlaybackProgress lastProgressByUrn = playSessionStateProvider.getLastProgressByUrn(playQueueManager.getCurrentTrackUrn());
                playbackOperations.playCurrent(lastProgressByUrn.getPosition());
            }
        }
    }

    @Override
    public void onRemoteMediaPlayerStatusUpdated() {
        Log.i(TAG, "On Status updated, status: " + videoCastManager.getRemoteMediaPlayer().getMediaStatus().getPlayerState());
        super.onRemoteMediaPlayerStatusUpdated();
    }

    @Override
    public void onRemoteMediaPlayerMetadataUpdated() {
        final Urn currentUrn = getRemoteUrn();
        final List<Urn> remoteTrackList = getRemoteTrackList();
        final int position = remoteTrackList.indexOf(currentUrn);
        Log.i(TAG, String.format("Remote media updated, CurrentUrn: %s, RemoteTrackListSize: %d", currentUrn.toString(), remoteTrackList.size()));

        if (!remoteTrackList.isEmpty()) {
            if (!playQueueManager.hasSameTrackList(remoteTrackList)) {
                Log.i(TAG, "Does not have the same tracklist, updating locally");
                final PlayQueue playQueue = PlayQueue.fromTrackUrnList(remoteTrackList.isEmpty() ? Arrays.asList(currentUrn) : remoteTrackList, PlaySessionSource.EMPTY);
                playQueueManager.setNewPlayQueue(playQueue, position, PlaySessionSource.EMPTY);
                playbackOperations.playCurrent();
                eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.showPlayer());
            } else {
                Log.i(TAG, "Has the same tracklist, setting position");
                playQueueManager.setPosition(position);
                if (videoCastManager.getPlaybackStatus() == MediaStatus.PLAYER_STATE_PLAYING) {
                    playbackOperations.playCurrent();
                }
            }
        }
    }

    private Urn getRemoteUrn() {
        try {
            final MediaInfo remoteMediaInformation = videoCastManager.getRemoteMediaInformation();
            Log.i(TAG, "Getting remote media info " + remoteMediaInformation);
            return CastPlayer.getUrnFromMediaMetadata(remoteMediaInformation);
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(TAG, "Unable to get remote media information", e);
        }
        return Urn.NOT_SET;
    }

    private List<Urn> getRemoteTrackList() {
        try {
            final MediaInfo remoteMediaInformation = videoCastManager.getRemoteMediaInformation();
            Log.i(TAG, "Got media information " + remoteMediaInformation);
            if (remoteMediaInformation != null){
                final JSONObject customData = remoteMediaInformation.getCustomData();
                Log.i(TAG, "Has custom data object " + customData);
                if (customData != null){
                    return convertRemoteDataToTrackList(customData);
                }
            }
        } catch (TransientNetworkDisconnectionException | NoConnectionException | JSONException e) {
            Log.e(TAG, "Unable to retrieve remote play queue", e);
        }
        return Collections.emptyList();
    }

    private List<Urn> convertRemoteDataToTrackList(JSONObject customData) throws JSONException {
        final JSONArray jsonArray = (JSONArray) customData.get(CastPlayer.KEY_PLAY_QUEUE);
        Log.i(TAG, "Has tracklist of length " + jsonArray.length());
        List<Urn> remoteList = new ArrayList<>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++){
            remoteList.add(new Urn(jsonArray.getString(i)));
        }
        return remoteList;
    }
}
