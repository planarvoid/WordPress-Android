package com.soundcloud.android.cast;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class CastSessionControllerTest {

    private static final Urn URN = Urn.forTrack(123L);
    private static final Urn URN2 = Urn.forTrack(456L);
    private static final List<Urn> PLAY_QUEUE = Arrays.asList(URN, URN2);

    private CastSessionController castSessionController;

    @Mock private CastOperations castOperations;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private VideoCastManager videoCastManager;
    @Mock private PlaySessionStateProvider playSessionStateProvider;

    private final TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        castSessionController = new CastSessionController(castOperations,
                playbackOperations,
                playQueueManager,
                videoCastManager,
                eventBus,
                playSessionStateProvider);
        when(castOperations.loadRemotePlayQueue()).thenReturn(new RemotePlayQueue(Collections.<Urn>emptyList(), Urn.NOT_SET));
    }

    @Test
    public void isNotListeningByDefault() throws Exception {
        verify(videoCastManager, never()).addVideoCastConsumer(any(VideoCastConsumer.class));
    }

    @Test
    public void onConnectedToReceiverAppDoesNotReloadAndPlayCurrentQueueIfQueueIsEmpty() throws Exception {
        castSessionController.startListening();
        when(playQueueManager.isQueueEmpty()).thenReturn(true);

        callOnConnectedToReceiverApp();

        verify(playbackOperations, never()).reloadAndPlayCurrentQueue(anyInt());
    }

    @Test
    public void onConnectedToReceiverAppStopsPlaybackService() throws Exception {
        castSessionController.startListening();
        when(playSessionStateProvider.getLastProgressByUrn(any(Urn.class))).thenReturn(PlaybackProgress.empty());

        callOnConnectedToReceiverApp();

        verify(playbackOperations).stopService();
    }

    @Test
    public void onConnectedToReceiverAppReloadsAndPlaysCurrentTrackFromLastPosition() throws Exception {
        castSessionController.startListening();
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(URN);
        when(playSessionStateProvider.getLastProgressByUrn(URN)).thenReturn(new PlaybackProgress(123, 456));

        callOnConnectedToReceiverApp();

        verify(playbackOperations).reloadAndPlayCurrentQueue(123L);
    }

    @Test
    public void onMetaDataUpdatedDoesNotSetPlayQueueWithSameTrackList() throws Exception {
        castSessionController.startListening();
        when(playQueueManager.hasSameTrackList(PLAY_QUEUE)).thenReturn(true);

        callOnMetadatUpdated();

        verify(playQueueManager, never()).setNewPlayQueue(any(PlayQueue.class), anyInt(), any(PlaySessionSource.class));
    }

    @Test
    public void onMetaDataUpdatedPlaysCurrentTrackWithSameRemoteQueueAndRemoteIsPlaying() throws Exception {
        castSessionController.startListening();
        when(castOperations.loadRemotePlayQueue()).thenReturn(new RemotePlayQueue(PLAY_QUEUE, URN));
        when(playQueueManager.hasSameTrackList(PLAY_QUEUE)).thenReturn(true);
        when(videoCastManager.getPlaybackStatus()).thenReturn(MediaStatus.PLAYER_STATE_PLAYING);

        callOnMetadatUpdated();

        verify(playbackOperations).playCurrent();
    }

    @Test
    public void onMetaDataUpdatedSetsPlayQueueWithDifferentTracklist() throws Exception {
        castSessionController.startListening();
        when(castOperations.loadRemotePlayQueue()).thenReturn(new RemotePlayQueue(PLAY_QUEUE, URN));

        callOnMetadatUpdated();

        expect(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).size()).toEqual(1);
        expect(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).get(0).isShow()).toBeTrue();
    }

    @Test
    public void onMetaDataUpdatedPlaysCurrentTrackWhenRemotePlayQueueIsDifferent() throws Exception {
        castSessionController.startListening();
        when(castOperations.loadRemotePlayQueue()).thenReturn(new RemotePlayQueue(PLAY_QUEUE, URN));

        callOnMetadatUpdated();

        verify(playbackOperations).playCurrent();
    }

    @Test
    public void onMetaDataUpdatedShowsPlayer() throws Exception {
        castSessionController.startListening();
        when(castOperations.loadRemotePlayQueue()).thenReturn(new RemotePlayQueue(PLAY_QUEUE, URN));

        callOnMetadatUpdated();

        expect(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).size()).toEqual(1);
        expect(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).get(0).isShow()).toBeTrue();
    }

    private void callOnMetadatUpdated() {
        castSessionController.onRemoteMediaPlayerMetadataUpdated();
    }

    private void callOnConnectedToReceiverApp() {
        castSessionController.onApplicationConnected(null, null, true);
    }
}