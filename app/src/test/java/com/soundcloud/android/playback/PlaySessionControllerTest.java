package com.soundcloud.android.playback;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlaybackProgressEvent;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
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
    @Mock
    private PlayQueueManager playQueueManager;
    @Mock
    private Context context;

    private  PlaySessionController controller;
    private EventMonitor monitor;

    @Before
    public void setUp() throws Exception {
        controller = new PlaySessionController(context, eventBus, playbackOperations, playQueueManager);
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

        verify(playbackOperations).playCurrent(context);
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

    @Test
    public void onStateTransitionDoesNotTryToAdvanceTracksIfTrackNotEnded() throws Exception {
        monitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE));
        monitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE));
        monitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.ERROR_FAILED));
        verifyZeroInteractions(playQueueManager);
    }

    @Test
    public void onStateTransitionPlaysCurrentTrackAfterAdvancingPlayQueueAfterCompletedTrack() throws Exception {
        when(playQueueManager.autoNextTrack()).thenReturn(true);
        monitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE));
        verify(playbackOperations).playCurrent(context);
    }

    @Test
    public void onStateTransitionDoesNotOpenCurrentTrackAfterFailingToAdvancePlayQueue() throws Exception {
        when(playQueueManager.autoNextTrack()).thenReturn(false);
        monitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE));
        verifyZeroInteractions(playbackOperations);
    }

    @Test
    public void onStateTransitionPublishesPlayQueueCompleteEventAfterFailingToAdvancePlayQueue() throws Exception {
        when(playQueueManager.autoNextTrack()).thenReturn(false);
        monitor.publish(EventQueue.PLAYBACK_STATE_CHANGED, new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.TRACK_COMPLETE));
        Playa.StateTransition event = monitor.verifyEventOn(EventQueue.PLAYBACK_STATE_CHANGED);
        expect(event.getNewState()).toBe(Playa.PlayaState.IDLE);
        expect(event.getReason()).toBe(Playa.Reason.PLAY_QUEUE_COMPLETE);
    }
}