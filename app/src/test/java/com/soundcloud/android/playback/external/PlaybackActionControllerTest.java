package com.soundcloud.android.playback.external;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.ads.AdsController;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlaybackActionControllerTest extends AndroidUnitTest {

    private PlaybackActionController controller;

    @Mock private PlaySessionController playSessionController;
    @Mock private PlaybackServiceController serviceInitiator;
    @Mock private AdsController adsController;

    @Before
    public void setup() {
        controller = new PlaybackActionController(playSessionController, serviceInitiator, adsController);
    }

    @Test
    public void shouldGoToPreviousTrackWhenPreviousPlaybackAction() throws Exception {
        controller.handleAction(PlaybackAction.PREVIOUS, "source");

        verify(playSessionController).previousTrack();
    }

    @Test
    public void shouldGoToNextTrackWhenNextPlaybackActionIsHandled() {
        controller.handleAction(PlaybackAction.NEXT, "source");

        verify(playSessionController).nextTrack();
    }

    @Test
    public void closeActionCallsStopServiceOnPlaybackOperations() {
        controller.handleAction(PlaybackAction.CLOSE, "source");

        verify(serviceInitiator).stopPlaybackService();
    }

    @Test
    public void shouldTogglePlaybackWhenTogglePlaybackActionIsHandled() {
        controller.handleAction(PlaybackAction.TOGGLE_PLAYBACK, "source");

        verify(playSessionController).togglePlayback();
    }

    @Test
    public void shouldPlayWhenPlayActionIsHandled() {
        controller.handleAction(PlaybackAction.PLAY, "source");
        verify(playSessionController).play();
    }

    @Test
    public void shouldPauseWhenPauseActionIsHandled() {
        controller.handleAction(PlaybackAction.PAUSE, "source");
        verify(playSessionController).pause();
    }

    @Test
    public void shouldReconfigureAdAndAttemptAdDeliveryEventPublishIfTrackSkipFromNotification() {
        controller.handleAction(PlaybackAction.NEXT, PlaybackActionReceiver.SOURCE_REMOTE);

        verify(adsController).reconfigureAdForNextTrack();
        verify(adsController).publishAdDeliveryEventIfUpcoming();
    }

    @Test
    public void shouldReconfigureAdAndAttemptAdDeliveryEventPublishIfTrackSkipFromWidget() {
        controller.handleAction(PlaybackAction.NEXT, PlaybackActionReceiver.SOURCE_WIDGET);

        verify(adsController).reconfigureAdForNextTrack();
        verify(adsController).publishAdDeliveryEventIfUpcoming();
    }
}
