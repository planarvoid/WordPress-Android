package com.soundcloud.android.playback.service;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class BufferUnderrunListenerTest {
    private BufferUnderrunListener listener;
    private TestEventBus eventBus;
    @Mock private BufferUnderrunListener.Detector detector;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        listener = new BufferUnderrunListener(detector, eventBus);
    }

    @Test
    public void sendBufferUnderrunEvent() {
        final Playa.StateTransition stateTransition = new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE, Urn.forTrack(123L));
        when(detector.onStateTransitionEvent(stateTransition)).thenReturn(true);

        listener.onPlaystateChanged(stateTransition);

        final List<TrackingEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.TRACKING);
        expect(playbackPerformanceEvents.size()).toEqual(1);
    }

    @Test
    public void shouldSendBufferUnderrunEventWhenBufferingDuringPlayback() {
        final Playa.StateTransition stateTransition = new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE, Urn.forTrack(123L));
        when(detector.onStateTransitionEvent(stateTransition)).thenReturn(false);

        listener.onPlaystateChanged(stateTransition);

        final List<TrackingEvent> playbackPerformanceEvents = eventBus.eventsOn(EventQueue.TRACKING);
        expect(playbackPerformanceEvents).toBeEmpty();
    }
}