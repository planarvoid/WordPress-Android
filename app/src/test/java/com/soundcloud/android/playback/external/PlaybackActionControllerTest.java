package com.soundcloud.android.playback.external;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackActionControllerTest {

    private PlaybackActionController controller;

    private TestEventBus eventBus = new TestEventBus();

    @Mock
    private PlaybackOperations playbackOperations;
    @Mock
    private PlaySessionController playSessionController;

    @Before
    public void setup() {
        controller = new PlaybackActionController(playbackOperations, playSessionController, eventBus);
    }

    @Test
    public void shouldGoToPreviousTrackWhenPreviousPlaybackActionIsHandled() throws Exception {
        controller.handleAction(PlaybackAction.PREVIOUS, "source");

        verify(playbackOperations).previousTrack();
    }

    @Test
    public void shouldTrackPreviousEventWithSource() {
        controller.handleAction(PlaybackAction.PREVIOUS, "source");

        PlayControlEvent event = eventBus.lastEventOn(EventQueue.PLAY_CONTROL);
        expect(event.getAttributes().get("action")).toEqual("prev");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

    @Test
    public void shouldGoToNextTrackWhenNextPlaybackActionIsHandled() throws Exception {
        controller.handleAction(PlaybackAction.NEXT, "source");

        verify(playbackOperations).nextTrack();
    }

    @Test
    public void shouldTrackSkipEventWithSource() {
        controller.handleAction(PlaybackAction.NEXT, "source");

        PlayControlEvent event = eventBus.lastEventOn(EventQueue.PLAY_CONTROL);
        expect(event.getAttributes().get("action")).toEqual("skip");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

    @Test
    public void shouldTogglePlaybackWhenTogglePlaybackActionIsHandled() throws Exception {
        controller.handleAction(PlaybackAction.TOGGLE_PLAYBACK, "source");

        verify(playbackOperations).togglePlayback();
    }

    @Test
    public void shouldTrackTogglePlayEventWithSource() {
        when(playSessionController.isPlaying()).thenReturn(false);
        controller.handleAction(PlaybackAction.TOGGLE_PLAYBACK, "source");

        PlayControlEvent event = eventBus.lastEventOn(EventQueue.PLAY_CONTROL);
        expect(event.getAttributes().get("action")).toEqual("play");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

    @Test
    public void shouldTrackTogglePauseEventWithSource() {
        when(playSessionController.isPlaying()).thenReturn(true);
        controller.handleAction(PlaybackAction.TOGGLE_PLAYBACK, "source");

        PlayControlEvent event = eventBus.lastEventOn(EventQueue.PLAY_CONTROL);
        expect(event.getAttributes().get("action")).toEqual("pause");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

}