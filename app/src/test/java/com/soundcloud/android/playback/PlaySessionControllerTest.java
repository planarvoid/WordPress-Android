package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Context;

@RunWith(SoundCloudTestRunner.class)
public class PlaySessionControllerTest {

    @Mock
    private EventBus eventBus;
    @Mock
    private PlaybackOperations playbackOperations;

    private  PlaySessionController controller;
    private EventMonitor monitor;

    @Before
    public void setUp() throws Exception {
        controller = new PlaySessionController(Robolectric.application, eventBus, playbackOperations);
        monitor = EventMonitor.on(eventBus);
        controller.subscribe();
    }

    @Test
    public void shouldSubscribeToThePlayStateEventQueue() {
        monitor.verifySubscribedTo(EventQueue.PLAYBACK_STATE_CHANGED);
    }

    @Test
    public void shouldSubscribeToThePlaybackProgressEventQueue() {
        monitor.verifySubscribedTo(EventQueue.PLAYBACK_PROGRESS);
    }

    @Test
    public void shouldSubscribeToThePlayQueueEventQueue() {
        monitor.verifySubscribedTo(EventQueue.PLAY_QUEUE);
    }

    @Test
    public void shouldPlayCurrentTrackWhenTheQueueChangeIfThePlayerIsAlreadyPlaying() {
        monitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE));
        monitor.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange());

        verify(playbackOperations).playCurrent(Robolectric.application);
    }

    @Test
    public void shouldNotRespondToQueueChangesWhenPlayerIsNotPlaying() {
        monitor.publish(EventQueue.PLAY_QUEUE, PlayQueueEvent.fromTrackChange());

        verify(playbackOperations, never()).playCurrent(any(Context.class));
    }

    @Test
    public void returnsLastProgressEventFromEventQueue() throws Exception {
        final PlaybackProgressEvent playbackProgressEvent = new PlaybackProgressEvent(1L, 2L);
        monitor.publish(EventQueue.PLAYBACK_PROGRESS, playbackProgressEvent);
        expect(controller.getCurrentProgress()).toBe(playbackProgressEvent);

    }
}