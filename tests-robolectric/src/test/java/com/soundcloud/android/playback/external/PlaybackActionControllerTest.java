package com.soundcloud.android.playback.external;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackActionControllerTest {

    private PlaybackActionController controller;

    private TestEventBus eventBus = new TestEventBus();

    @Mock private PlaybackOperations playbackOperations;
    @Mock private PlaySessionStateProvider playSessionStateProvider;

    @Before
    public void setup() {
        controller = new PlaybackActionController(playbackOperations, playSessionStateProvider, eventBus);
    }

    @Test
    public void shouldGoToPreviousTrackWhenPreviousPlaybackAction() throws Exception {
        controller.handleAction(PlaybackAction.PREVIOUS, "source");

        verify(playbackOperations).previousTrack();
    }

    @Test
    public void shouldTrackPreviousEventWithSource() {
        controller.handleAction(PlaybackAction.PREVIOUS, "source");

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.getAttributes().get("action")).toEqual("prev");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

    @Test
    public void shouldGoToNextTrackWhenNextPlaybackActionIsHandled() {
        controller.handleAction(PlaybackAction.NEXT, "source");

        verify(playbackOperations).nextTrack();
    }

    @Test
    public void closeActionCallsStopServiceOnPlaybackOperations() {
        controller.handleAction(PlaybackAction.CLOSE, "source");

        verify(playbackOperations).stopService();
    }

    @Test
    public void closeActionCallsTracksCloseEventWithSource() {
        controller.handleAction(PlaybackAction.CLOSE, "source");

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.getAttributes().get("action")).toEqual("close");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

    @Test
    public void shouldTrackSkipEventWithSource() {
        controller.handleAction(PlaybackAction.NEXT, "source");

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.getAttributes().get("action")).toEqual("skip");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

    @Test
    public void shouldTogglePlaybackWhenTogglePlaybackActionIsHandled() {
        controller.handleAction(PlaybackAction.TOGGLE_PLAYBACK, "source");

        verify(playbackOperations).togglePlayback();
    }

    @Test
    public void shouldTrackTogglePlayEventWithSource() {
        when(playSessionStateProvider.isPlaying()).thenReturn(false);
        controller.handleAction(PlaybackAction.TOGGLE_PLAYBACK, "source");

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.getAttributes().get("action")).toEqual("play");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

    @Test
    public void shouldTrackTogglePauseEventWithSource() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);
        controller.handleAction(PlaybackAction.TOGGLE_PLAYBACK, "source");

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.getAttributes().get("action")).toEqual("pause");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

    @Test
    public void shouldPlayWhenPlayActionIsHandled() {
        controller.handleAction(PlaybackAction.PLAY, "source");
        verify(playbackOperations).play();
    }

    @Test
    public void shouldTrackPlayEventWithSource() {
        controller.handleAction(PlaybackAction.PLAY, "source");

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.getAttributes().get("action")).toEqual("play");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

    @Test
    public void shouldPauseWhenPauseActionIsHandled() {
        controller.handleAction(PlaybackAction.PAUSE, "source");
        verify(playbackOperations).pause();
    }

    @Test
    public void shouldTrackPauseEventWithSource() {
        controller.handleAction(PlaybackAction.PAUSE, "source");

        TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        expect(event.getAttributes().get("action")).toEqual("pause");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

}