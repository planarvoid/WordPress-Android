package com.soundcloud.android.cast;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.framework.CastSession;
import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;

public class CastSessionControllerTest extends AndroidUnitTest {

    private DefaultCastSessionController castSessionController;

    @Mock private PlaybackServiceController serviceInitiator;
    @Mock private DefaultCastPlayer castPlayer;
    @Mock private CastContextWrapper castContext;
    @Mock private CastSession castSession;
    @Mock private CastDevice castDevice;
    @Mock private CastConnectionHelper castConnectionHelper;
    @Mock private CastProtocol castProtocol;
    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private AdsOperations adsOperations;

    @Before
    public void setUp() throws Exception {
        castSessionController = new DefaultCastSessionController(serviceInitiator,
                                                                 adsOperations,
                                                                 castPlayer,
                                                                 castContext,
                                                                 castConnectionHelper,
                                                                 playSessionStateProvider,
                                                                 castProtocol);
        when(castSession.getCastDevice()).thenReturn(castDevice);
        when(castDevice.getFriendlyName()).thenReturn("My Cast");
    }

    @Test
    public void isNotListeningByDefault() {
        verify(castContext, never()).addCastStateListener(any(DefaultCastSessionController.class));
    }

    @Test
    public void onConnectedToReceiverAppStopsPlaybackService() {
        castSessionController.startListening();

        callOnConnectedToReceiverApp();

        verify(serviceInitiator).stopPlaybackService();
    }

    @Test
    public void onConnectedToReceiverAppCastPlayerGetsConnected() {
        castSessionController.startListening();

        callOnConnectedToReceiverApp();

        verify(castPlayer).onConnected(anyBoolean());
    }

    @Test
    public void onConnectedToReceiverChannelIsRegistered() throws IOException {
        castSessionController.startListening();

        callOnConnectedToReceiverApp();

        verify(castProtocol).registerCastSession(castSession);
    }

    @Test
    public void onSessionStartedClearsAllAdsFromQueue() {
        callOnConnectedToReceiverApp();

        verify(adsOperations).clearAllAdsFromQueue();
    }

    @Test
    public void onConnectedForwardsTheSessionStateBeforePlaybackIsForcefullyStopped() {
        final boolean isPlaying = true;
        when(playSessionStateProvider.isPlaying()).thenReturn(isPlaying);
        castSessionController.startListening();

        callOnConnectedToReceiverApp();

        verify(castPlayer).onConnected(isPlaying);
    }

    private void callOnConnectedToReceiverApp() {
        castSessionController.onSessionStarted(castSession, "0L");
    }
}
