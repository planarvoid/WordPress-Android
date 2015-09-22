package com.soundcloud.android.playback.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ServiceInitiator;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlaybackActionControllerTest extends AndroidUnitTest {

    private PlaybackActionController controller;

    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlaySessionController playSessionController;
    @Mock private ServiceInitiator serviceInitiator;
    @Mock private PlaySessionStateProvider playSessionStateProvider;

    @Before
    public void setup() {
        controller = new PlaybackActionController(playSessionController, serviceInitiator, playSessionStateProvider, eventBus);
    }

    @Test
    public void shouldGoToPreviousTrackWhenPreviousPlaybackAction() throws Exception {
        controller.handleAction(PlaybackAction.PREVIOUS, "source");

        verify(playSessionController).previousTrack();
    }

    @Test
    public void shouldTrackPreviousEventWithSource() {
        controller.handleAction(PlaybackAction.PREVIOUS, "source");

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getAttributes().get("action")).isEqualTo("prev");
        assertThat(event.getAttributes().get("location")).isEqualTo("source");
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
    public void closeActionCallsTracksCloseEventWithSource() {
        controller.handleAction(PlaybackAction.CLOSE, "source");

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getAttributes().get("action")).isEqualTo("close");
        assertThat(event.getAttributes().get("location")).isEqualTo("source");
    }

    @Test
    public void shouldTrackSkipEventWithSource() {
        controller.handleAction(PlaybackAction.NEXT, "source");

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getAttributes().get("action")).isEqualTo("skip");
        assertThat(event.getAttributes().get("location")).isEqualTo("source");
    }

    @Test
    public void shouldTogglePlaybackWhenTogglePlaybackActionIsHandled() {
        controller.handleAction(PlaybackAction.TOGGLE_PLAYBACK, "source");

        verify(playSessionController).togglePlayback();
    }

    @Test
    public void shouldTrackTogglePlayEventWithSource() {
        when(playSessionStateProvider.isPlaying()).thenReturn(false);
        controller.handleAction(PlaybackAction.TOGGLE_PLAYBACK, "source");

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getAttributes().get("action")).isEqualTo("play");
        assertThat(event.getAttributes().get("location")).isEqualTo("source");
    }

    @Test
    public void shouldTrackTogglePauseEventWithSource() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        controller.handleAction(PlaybackAction.TOGGLE_PLAYBACK, "source");

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getAttributes().get("action")).isEqualTo("pause");
        assertThat(event.getAttributes().get("location")).isEqualTo("source");
    }

    @Test
    public void shouldPlayWhenPlayActionIsHandled() {
        controller.handleAction(PlaybackAction.PLAY, "source");
        verify(playSessionController).play();
    }

    @Test
    public void shouldTrackPlayEventWithSource() {
        controller.handleAction(PlaybackAction.PLAY, "source");

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getAttributes().get("action")).isEqualTo("play");
        assertThat(event.getAttributes().get("location")).isEqualTo("source");
    }

    @Test
    public void shouldPauseWhenPauseActionIsHandled() {
        controller.handleAction(PlaybackAction.PAUSE, "source");
        verify(playSessionController).pause();
    }

    @Test
    public void shouldTrackPauseEventWithSource() {
        controller.handleAction(PlaybackAction.PAUSE, "source");

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getAttributes().get("action")).isEqualTo("pause");
        assertThat(event.getAttributes().get("location")).isEqualTo("source");
    }

}