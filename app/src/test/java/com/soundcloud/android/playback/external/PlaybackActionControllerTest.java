package com.soundcloud.android.playback.external;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.Context;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackActionControllerTest {

    private PlaybackActionController controller;

    @Mock
    private EventBus eventBus;
    @Mock
    private PlaybackOperations playbackOperations;
    @Mock
    private PlaySessionController playSessionController;

    @Mock
    Context context;
    @Captor
    ArgumentCaptor<PlayControlEvent> captor;

    @Before
    public void setup() {
        controller = new PlaybackActionController(playbackOperations, playSessionController, eventBus);
    }

    @Test
    public void shouldGoToPreviousTrackWhenPreviousPlaybackActionIsHandled() throws Exception {
        controller.handleAction(context, PlaybackAction.PREVIOUS, "source");

        verify(playbackOperations).previousTrack();
    }

    @Test
    public void shouldTrackPreviousEventWithSource() {
        controller.handleAction(context, PlaybackAction.PREVIOUS, "source");

        verify(eventBus).publish(eq(EventQueue.PLAY_CONTROL), captor.capture());

        PlayControlEvent event = captor.getValue();
        expect(event.getAttributes().get("action")).toEqual("prev");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

    @Test
    public void shouldGoToNextTrackWhenNextPlaybackActionIsHandled() throws Exception {
        controller.handleAction(context, PlaybackAction.NEXT, "source");

        verify(playbackOperations).nextTrack();
    }

    @Test
    public void shouldTrackSkipEventWithSource() {
        controller.handleAction(context, PlaybackAction.NEXT, "source");

        verify(eventBus).publish(eq(EventQueue.PLAY_CONTROL), captor.capture());
        PlayControlEvent event = captor.getValue();
        expect(event.getAttributes().get("action")).toEqual("skip");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

    @Test
    public void shouldTogglePlaybackWhenTogglePlaybackActionIsHandled() throws Exception {
        controller.handleAction(context, PlaybackAction.TOGGLE_PLAYBACK, "source");

        verify(playbackOperations).togglePlayback();
    }


    @Test
    public void shouldTrackTogglePlayEventWithSource() {
        when(playSessionController.isPlaying()).thenReturn(false);
        controller.handleAction(context, PlaybackAction.TOGGLE_PLAYBACK, "source");

        verify(eventBus).publish(eq(EventQueue.PLAY_CONTROL), captor.capture());
        PlayControlEvent event = captor.getValue();
        expect(event.getAttributes().get("action")).toEqual("play");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

    @Test
    public void shouldTrackTogglePauseEventWithSource() {
        when(playSessionController.isPlaying()).thenReturn(true);
        controller.handleAction(context, PlaybackAction.TOGGLE_PLAYBACK, "source");

        verify(eventBus).publish(eq(EventQueue.PLAY_CONTROL), captor.capture());
        PlayControlEvent event = captor.getValue();
        expect(event.getAttributes().get("action")).toEqual("pause");
        expect(event.getAttributes().get("location")).toEqual("source");
    }

}