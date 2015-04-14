package com.soundcloud.android.cast;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumer;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.PlayQueue;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import java.util.Arrays;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class CastSessionControllerTest {

    private static final Urn URN = Urn.forTrack(123L);
    private static final Urn URN2 = Urn.forTrack(456L);
    private static final List<Urn> PLAY_QUEUE = Arrays.asList(URN, URN2);

    private CastSessionController castSessionController;

    @Mock private PlaybackOperations playbackOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private VideoCastManager videoCastManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;

    private final TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        castSessionController = new CastSessionController(playbackOperations, playQueueManager, videoCastManager, eventBus, playSessionStateProvider);
        when(playbackOperations.playTrackWithRecommendations(any(Urn.class), any(PlaySessionSource.class))).thenReturn(Observable.<List<Urn>>empty());
    }

    @Test
    public void isNotListeningByDefault() throws Exception {
        verify(videoCastManager, never()).addVideoCastConsumer(any(VideoCastConsumer.class));
    }

    @Test
    public void onConnectedToReceiverAppDoesNothingIfNotPlaying() throws Exception {
        castSessionController.startListening();

        callOnConnectedToReceiverApp();

        verifyZeroInteractions(playbackOperations);
    }

    @Test
    public void onConnectedToReceiverAppStopsPlaybackServiceIfPlaying() throws Exception {
        castSessionController.startListening();
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(URN);
        when(playSessionStateProvider.getLastProgressByUrn(URN)).thenReturn(new PlaybackProgress(123, 456));

        callOnConnectedToReceiverApp();

        verify(playbackOperations).stopService();
    }

    @Test
    public void onConnectedToReceiverAppPlaysCurrentTrackFromLastPosition() throws Exception {
        castSessionController.startListening();
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(URN);
        when(playSessionStateProvider.getLastProgressByUrn(URN)).thenReturn(new PlaybackProgress(123, 456));

        callOnConnectedToReceiverApp();

        verify(playbackOperations).playCurrent(123);
    }

    @Test
    public void onMetaDataUpdatedDoesNotSetPlayQueueWithSameTrackList() throws Exception {
        castSessionController.startListening();
        when(playQueueManager.hasSameTrackList(PLAY_QUEUE)).thenReturn(true);
        setupExistingCastSession();

        callOnMetadatUpdated();

        verify(playQueueManager, never()).setNewPlayQueue(any(PlayQueue.class), anyInt(), any(PlaySessionSource.class));
    }

    @Test
    public void onMetaDataUpdatedPlaysCurrentTrackWithSameRemoteQueueAndRemoteIsPlaying() throws Exception {
        castSessionController.startListening();
        when(playQueueManager.hasSameTrackList(PLAY_QUEUE)).thenReturn(true);
        when(videoCastManager.getPlaybackStatus()).thenReturn(MediaStatus.PLAYER_STATE_PLAYING);
        setupExistingCastSession();

        callOnMetadatUpdated();

        verify(playbackOperations).playCurrent();
    }

    @Test
    public void onMetaDataUpdatedStopsPlaybackServiceWithSameRemoteQueueAndRemoteIsNotPlaying() throws Exception {
        castSessionController.startListening();
        when(playQueueManager.hasSameTrackList(PLAY_QUEUE)).thenReturn(true);
        when(videoCastManager.getPlaybackStatus()).thenReturn(MediaStatus.PLAYER_STATE_UNKNOWN);
        setupExistingCastSession();

        callOnMetadatUpdated();

        verify(playbackOperations).stopService();
    }

    @Test
    public void onMetaDataUpdatedSetsPlayQueueWithDifferentTracklist() throws Exception {
        castSessionController.startListening();
        setupExistingCastSession();

        callOnMetadatUpdated();

        expect(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).size()).toEqual(1);
        expect(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).get(0).isShow()).toBeTrue();
    }

    @Test
    public void onMetaDataUpdatedPlaysCurrentTrackWhenRemotePlayQueueIsDifferent() throws Exception {
        castSessionController.startListening();
        setupExistingCastSession();

        callOnMetadatUpdated();

        verify(playbackOperations).playCurrent();
    }

    @Test
    public void onMetaDataUpdatedShowsPlayer() throws Exception {
        castSessionController.startListening();
        setupExistingCastSession();

        callOnMetadatUpdated();

        expect(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).size()).toEqual(1);
        expect(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).get(0).isShow()).toBeTrue();
    }

    private void setupExistingCastSession() throws TransientNetworkDisconnectionException, NoConnectionException {
        MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        mediaMetadata.putString(CastPlayer.KEY_URN, URN.toString());

        JSONObject playQueue = new JSONObject();
        try {
            playQueue.put(CastPlayer.KEY_PLAY_QUEUE, PLAY_QUEUE);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        when(videoCastManager.getRemoteMediaInformation()).thenReturn(new MediaInfo.Builder("content-id")
                .setMetadata(mediaMetadata)
                .setCustomData(playQueue)
                .setContentType("audio/mpeg")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .build());
    }

    private void callOnMetadatUpdated() {
        castSessionController.onRemoteMediaPlayerMetadataUpdated();
    }

    private void callOnConnectedToReceiverApp() {
        castSessionController.onApplicationConnected(null, null, true);
    }
}