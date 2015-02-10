package com.soundcloud.android.cast;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.cast.CastConnectionHelper.CastConnectionListener;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playback.ui.view.AdToastViewController;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class CastSessionReconnectorTest {

    private static final Urn URN = Urn.forTrack(123L);
    private CastSessionReconnector castSessionReconnector;

    @Mock private PlaybackOperations playbackOperations;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private CastConnectionHelper castConnectionHelper;
    @Mock private AdToastViewController adToastViewController;
    @Mock private PlaySessionStateProvider playSessionStateProvider;

    @Captor private ArgumentCaptor<CastConnectionListener> connectionListenerCaptor;

    private final TestObservables.MockObservable<List<Urn>> mockObservable = TestObservables.emptyObservable();
    private final TestEventBus eventBus = new TestEventBus();

    @Before
    public void setUp() throws Exception {
        castSessionReconnector = new CastSessionReconnector(playbackOperations, playQueueManager, castConnectionHelper, eventBus, adToastViewController, playSessionStateProvider);
        when(playbackOperations.playTrackWithRecommendations(any(Urn.class), any(PlaySessionSource.class))).thenReturn(Observable.<List<Urn>>empty());
    }

    @Test
    public void isNotListeningByDefault() throws Exception {
        verify(castConnectionHelper, never()).addConnectionListener(any(CastConnectionListener.class));
    }

    @Test
    public void onConnectedToReceiverAppDoesNothingIfNotPlaying() throws Exception {
        castSessionReconnector.startListening();

        callOnConnectedToReceiverApp();

        verifyZeroInteractions(playbackOperations);
    }

    @Test
    public void onConnectedToReceiverAppStopsPlaybackServiceIfPlaying() throws Exception {
        castSessionReconnector.startListening();
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(URN);
        when(playSessionStateProvider.getLastProgressByUrn(URN)).thenReturn(new PlaybackProgress(123, 456));

        callOnConnectedToReceiverApp();

        verify(playbackOperations).stopService();
    }

    @Test
    public void onConnectedToReceiverAppPlaysCurrentTrackFromLastPosition() throws Exception {
        castSessionReconnector.startListening();
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(URN);
        when(playSessionStateProvider.getLastProgressByUrn(URN)).thenReturn(new PlaybackProgress(123, 456));

        callOnConnectedToReceiverApp();

        verify(playbackOperations).playCurrent(123);
    }

    @Test
    public void onMetaDataUpdatedDoesNothingIfQueueNotEmpty() throws Exception {
        castSessionReconnector.startListening();

        callOnMetadatUpdated();

        verifyZeroInteractions(playbackOperations);
    }

    @Test
    public void onMetaDataUpdatedShowsPlayerAfterPlayingTrackWithRecommendations() throws Exception {
        castSessionReconnector.startListening();
        when(playQueueManager.isQueueEmpty()).thenReturn(true);
        when(playbackOperations.playTrackWithRecommendations(URN, PlaySessionSource.EMPTY)).thenReturn(mockObservable);

        callOnMetadatUpdated();

        expect(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).size()).toEqual(1);
        expect(eventBus.eventsOn(EventQueue.PLAYER_COMMAND).get(0).isShow()).toBeTrue();

    }

    private void callOnMetadatUpdated() {
        verify(castConnectionHelper).addConnectionListener(connectionListenerCaptor.capture());
        connectionListenerCaptor.getValue().onMetaDataUpdated(URN);
    }

    private void callOnConnectedToReceiverApp() {
        verify(castConnectionHelper).addConnectionListener(connectionListenerCaptor.capture());
        connectionListenerCaptor.getValue().onConnectedToReceiverApp();
    }
}