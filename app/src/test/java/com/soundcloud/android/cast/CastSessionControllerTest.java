package com.soundcloud.android.cast;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class CastSessionControllerTest extends AndroidUnitTest {

    private static final Urn URN = Urn.forTrack(123L);

    private CastSessionController castSessionController;

    @Mock private PlaybackServiceController serviceInitiator;
    @Mock private CastPlayer castPlayer;
    @Mock private PlaySessionController playSessionController;
    @Mock private CastContextWrapper castContext;
    @Mock private CastSession castSession;
    @Mock private CastDevice castDevice;

    @Before
    public void setUp() throws Exception {
        castSessionController = new CastSessionController(serviceInitiator,
                                                          castPlayer,
                                                          castContext,
                                                          playSessionController);
        when(castSession.getCastDevice()).thenReturn(castDevice);
        when(castDevice.getFriendlyName()).thenReturn("My Cast");
    }

    @Test
    public void isNotListeningByDefault() {
        verify(castContext, never()).addCastStateListener(any(CastSessionController.class));
    }

    @Test
    public void onSessionStartedDoesNotReloadWhenNotPlaying() {
        castSessionController.startListening();
        when(playSessionController.isPlayingCurrentPlayQueueItem()).thenReturn(false);

        callOnConnectedToReceiverApp();

        verify(castPlayer, never()).reloadCurrentQueue();
    }

    @Test
    public void onConnectedToReceiverAppStopsPlaybackService() {
        castSessionController.startListening();
        when(playSessionController.isPlayingCurrentPlayQueueItem()).thenReturn(true);

        callOnConnectedToReceiverApp();

        verify(serviceInitiator).stopPlaybackService();
    }

    @Test
    public void onConnectedToReceiverAppCastPlayerGetsConnected() {
        castSessionController.startListening();
        when(playSessionController.isPlayingCurrentPlayQueueItem()).thenReturn(true);

        callOnConnectedToReceiverApp();

        verify(castPlayer).onConnected(any(RemoteMediaClient.class));
    }

    @Test
    public void onConnectedToReceiverAppAttemptsToPlayLocalQueue() {
        castSessionController.startListening();
        when(playSessionController.isPlayingCurrentPlayQueueItem()).thenReturn(true);

        callOnConnectedToReceiverApp();

        verify(castPlayer).playLocalPlayQueueOnRemote();
    }

    private void callOnConnectedToReceiverApp() {
        castSessionController.onSessionStarted(castSession, "0L");
    }
}
