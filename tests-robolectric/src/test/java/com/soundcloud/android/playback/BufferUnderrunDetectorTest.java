package com.soundcloud.android.playback;

import static com.pivotallabs.greatexpectations.Expect.expect;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class BufferUnderrunDetectorTest {
    private BufferUnderrunListener.Detector detector;

    @Before
    public void setUp() throws Exception {
        detector = new BufferUnderrunListener.Detector();
    }

    @Test
    public void shouldDetectBufferUnderrun() {
        expect(detector.onStateTransitionEvent(bufferingEvent(Urn.forTrack(123L), 100))).toBeTrue();
    }

    @Test
    public void shouldNotDetectBufferUnderrunWhenPlaybackStarts() {
        expect(detector.onStateTransitionEvent(bufferingEvent(Urn.forTrack(123L), 0))).toBeFalse();
    }

    @Test
    public void shouldNotDetectBufferUnderrunWhenIdle() {
        expect(detector.onStateTransitionEvent(idleEvent(Urn.forTrack(123L)))).toBeFalse();
    }

    @Test
    public void shouldNotDetectBufferUnderrunWhenSeeking() {
        expect(detector.onStateTransitionEvent(playEvent(Urn.forTrack(123L)))).toBeFalse();
        detector.onSeek();
        expect(detector.onStateTransitionEvent(bufferingEvent(Urn.forTrack(123L), 100))).toBeFalse();
    }

    @Test
    public void shouldDetectBufferUnderrunWhenSeekingIsFinished() {
        expect(detector.onStateTransitionEvent(playEvent(Urn.forTrack(123L)))).toBeFalse();
        detector.onSeek();
        expect(detector.onStateTransitionEvent(bufferingEvent(Urn.forTrack(123L), 100))).toBeFalse();
        expect(detector.onStateTransitionEvent(playEvent(Urn.forTrack(123L)))).toBeFalse();
        expect(detector.onStateTransitionEvent(bufferingEvent(Urn.forTrack(123L), 100))).toBeTrue();
    }

    @Test
    public void shouldDetectSeveralBufferUnderrunInARow() {
        expect(detector.onStateTransitionEvent(playEvent(Urn.forTrack(123L)))).toBeFalse();
        expect(detector.onStateTransitionEvent(bufferingEvent(Urn.forTrack(123L), 100))).toBeTrue();
        expect(detector.onStateTransitionEvent(bufferingEvent(Urn.forTrack(123L), 100))).toBeTrue();
    }

    @Test
    public void shouldNotDetectBufferUnderrunWhenPlaybackStops() {
        expect(detector.onStateTransitionEvent(playEvent(Urn.forTrack(123L)))).toBeFalse();
        expect(detector.onStateTransitionEvent(idleEvent(Urn.forTrack(123L)))).toBeFalse();
    }

    private Playa.StateTransition bufferingEvent(Urn track, int position) {
        final int duration = position * 2;
        return new Playa.StateTransition(Playa.PlayaState.BUFFERING, Playa.Reason.NONE, track, position, duration);
    }

    private Playa.StateTransition playEvent(Urn track) {
        return new Playa.StateTransition(Playa.PlayaState.PLAYING, Playa.Reason.NONE, track);
    }

    private Playa.StateTransition idleEvent(Urn track) {
        return new Playa.StateTransition(Playa.PlayaState.IDLE, Playa.Reason.NONE, track);
    }
}