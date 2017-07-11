package com.soundcloud.android.playback.external;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.PlaybackServiceController;
import com.soundcloud.android.ads.PlayerAdsController;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaybackActionSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PlaybackActionControllerTest {

    private PlaybackActionController controller;

    @Mock private PlaySessionController playSessionController;
    @Mock private PlaybackServiceController serviceInitiator;
    @Mock private PlayerAdsController adsController;

    @Before
    public void setup() {
        controller = new PlaybackActionController(playSessionController, serviceInitiator, adsController);
    }

    @Test
    public void shouldGoToPreviousTrackWhenPreviousPlaybackAction() throws Exception {
        controller.handleAction(PlaybackAction.PREVIOUS, PlaybackActionSource.NOTIFICATION);

        verify(playSessionController).previousTrack();
    }

    @Test
    public void shouldGoToNextTrackWhenNextPlaybackActionIsHandled() {
        controller.handleAction(PlaybackAction.NEXT, PlaybackActionSource.NOTIFICATION);

        verify(playSessionController).nextTrack();
    }

    @Test
    public void closeActionCallsStopServiceOnPlaybackOperations() {
        controller.handleAction(PlaybackAction.CLOSE, PlaybackActionSource.NOTIFICATION);

        verify(serviceInitiator).stopPlaybackService();
    }

    @Test
    public void shouldTogglePlaybackWhenTogglePlaybackActionIsHandled() {
        controller.handleAction(PlaybackAction.TOGGLE_PLAYBACK, PlaybackActionSource.NOTIFICATION);

        verify(playSessionController).togglePlayback();
    }

    @Test
    public void shouldPlayWhenPlayActionIsHandled() {
        controller.handleAction(PlaybackAction.PLAY, PlaybackActionSource.NOTIFICATION);
        verify(playSessionController).play();
    }

    @Test
    public void shouldPauseWhenPauseActionIsHandled() {
        controller.handleAction(PlaybackAction.PAUSE, PlaybackActionSource.NOTIFICATION);
        verify(playSessionController).pause();
    }

    @Test
    public void shouldReconfigureAdAndAttemptAdDeliveryEventPublishIfTrackSkipFromNotification() {
        controller.handleAction(PlaybackAction.NEXT, PlaybackActionSource.NOTIFICATION);

        verify(adsController).reconfigureAdForNextTrack();
        verify(adsController).publishAdDeliveryEventIfUpcoming();
    }

    @Test
    public void shouldReconfigureAdAndAttemptAdDeliveryEventPublishIfTrackSkipFromWidget() {
        controller.handleAction(PlaybackAction.NEXT, PlaybackActionSource.WIDGET);

        verify(adsController).reconfigureAdForNextTrack();
        verify(adsController).publishAdDeliveryEventIfUpcoming();
    }
}
