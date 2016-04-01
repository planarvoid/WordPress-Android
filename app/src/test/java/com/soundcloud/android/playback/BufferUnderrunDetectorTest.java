package com.soundcloud.android.playback;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import org.junit.Before;
import org.junit.Test;

public class BufferUnderrunDetectorTest {
    private BufferUnderrunListener.Detector detector;

    @Before
    public void setUp() throws Exception {
        detector = new BufferUnderrunListener.Detector();
    }

    @Test
    public void shouldDetectBufferUnderrun() {
        assertThat(detector.onStateTransitionEvent(bufferingEvent(Urn.forTrack(123L), 100))).isTrue();
    }

    @Test
    public void shouldNotDetectBufferUnderrunWhenPlaybackStarts() {
        assertThat(detector.onStateTransitionEvent(bufferingEvent(Urn.forTrack(123L), 0))).isFalse();
    }

    @Test
    public void shouldNotDetectBufferUnderrunWhenIdle() {
        assertThat(detector.onStateTransitionEvent(idleEvent(Urn.forTrack(123L)))).isFalse();
    }

    @Test
    public void shouldNotDetectBufferUnderrunWhenSeeking() {
        assertThat(detector.onStateTransitionEvent(playEvent(Urn.forTrack(123L)))).isFalse();
        detector.onSeek();
        assertThat(detector.onStateTransitionEvent(bufferingEvent(Urn.forTrack(123L), 100))).isFalse();
    }

    @Test
    public void shouldDetectBufferUnderrunWhenSeekingIsFinished() {
        assertThat(detector.onStateTransitionEvent(playEvent(Urn.forTrack(123L)))).isFalse();
        detector.onSeek();
        assertThat(detector.onStateTransitionEvent(bufferingEvent(Urn.forTrack(123L), 100))).isFalse();
        assertThat(detector.onStateTransitionEvent(playEvent(Urn.forTrack(123L)))).isFalse();
        assertThat(detector.onStateTransitionEvent(bufferingEvent(Urn.forTrack(123L), 100))).isTrue();
    }

    @Test
    public void shouldDetectSeveralBufferUnderrunInARow() {
        assertThat(detector.onStateTransitionEvent(playEvent(Urn.forTrack(123L)))).isFalse();
        assertThat(detector.onStateTransitionEvent(bufferingEvent(Urn.forTrack(123L), 100))).isTrue();
        assertThat(detector.onStateTransitionEvent(bufferingEvent(Urn.forTrack(123L), 100))).isTrue();
    }

    @Test
    public void shouldNotDetectBufferUnderrunWhenPlaybackStops() {
        assertThat(detector.onStateTransitionEvent(playEvent(Urn.forTrack(123L)))).isFalse();
        assertThat(detector.onStateTransitionEvent(idleEvent(Urn.forTrack(123L)))).isFalse();
    }

    private PlaybackStateTransition bufferingEvent(Urn track, int position) {
        final int duration = position * 2;
        return new PlaybackStateTransition(PlaybackState.BUFFERING, PlayStateReason.NONE, track, position, duration);
    }

    private PlaybackStateTransition playEvent(Urn track) {
        return new PlaybackStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, track);
    }

    private PlaybackStateTransition idleEvent(Urn track) {
        return new PlaybackStateTransition(PlaybackState.IDLE, PlayStateReason.NONE, track);
    }
}
