package com.soundcloud.android.playback;

import static com.soundcloud.android.playback.StopReasonProvider.StopReason.STOP_REASON_BUFFERING;
import static com.soundcloud.android.playback.StopReasonProvider.StopReason.STOP_REASON_CONCURRENT_STREAMING;
import static com.soundcloud.android.playback.StopReasonProvider.StopReason.STOP_REASON_END_OF_QUEUE;
import static com.soundcloud.android.playback.StopReasonProvider.StopReason.STOP_REASON_ERROR;
import static com.soundcloud.android.playback.StopReasonProvider.StopReason.STOP_REASON_PAUSE;
import static com.soundcloud.android.playback.StopReasonProvider.StopReason.STOP_REASON_TRACK_FINISHED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.support.annotation.NonNull;

@RunWith(MockitoJUnitRunner.class)
public class StopReasonProviderTest {


    @Mock private PlayQueueManager playQueueManager;

    private StopReasonProvider provider;

    @Before
    public void setUp() throws Exception {
        provider = new StopReasonProvider(playQueueManager);
    }

    @Test
    public void stateChangeEventReturnsStopEventForTrackBuffering() throws Exception {
        final PlaybackStateTransition event = getStateTransition(PlaybackState.BUFFERING, PlayStateReason.NONE);
        assertThat(provider.fromTransition(event)).isEqualTo(STOP_REASON_BUFFERING);
    }

    @Test
    public void stateChangeEventReturnsStopEventForTrackPause() throws Exception {
        final PlaybackStateTransition event = getStateTransition(PlaybackState.IDLE, PlayStateReason.NONE);
        assertThat(provider.fromTransition(event)).isEqualTo(STOP_REASON_PAUSE);
    }

    @Test
    public void stateChangeEventReturnsStopEventForTrackError() throws Exception {
        final PlaybackStateTransition event = getStateTransition(PlaybackState.IDLE, PlayStateReason.ERROR_FAILED);
        assertThat(provider.fromTransition(event)).isEqualTo(STOP_REASON_ERROR);
    }

    @Test
    public void stateChangeEventReturnsStopEventForTrackFinished() throws Exception {
        final PlaybackStateTransition event = getStateTransition(PlaybackState.IDLE, PlayStateReason.PLAYBACK_COMPLETE);
        when(playQueueManager.hasNextItem()).thenReturn(true);
        assertThat(provider.fromTransition(event)).isEqualTo(STOP_REASON_TRACK_FINISHED);
    }

    @Test
    public void stateChangeEventReturnsStopEventForQueueFinished() throws Exception {
        final PlaybackStateTransition event = getStateTransition(PlaybackState.IDLE, PlayStateReason.PLAYBACK_COMPLETE);
        when(playQueueManager.hasNextItem()).thenReturn(false);
        assertThat(provider.fromTransition(event)).isEqualTo(STOP_REASON_END_OF_QUEUE);
    }

    @Test
    public void stateChangeEventReturnsStopEventForConcurrentStreaming() throws Exception {
        provider.setPendingConcurrentPause();
        final PlaybackStateTransition event = getStateTransition(PlaybackState.IDLE, PlayStateReason.NONE);
        assertThat(provider.fromTransition(event)).isEqualTo(STOP_REASON_CONCURRENT_STREAMING);
    }

    @Test
    public void stateChangeEventReturnsStopEventForPausedAfterGettingConcurrentStreamingReason() throws Exception {
        provider.setPendingConcurrentPause();
        final PlaybackStateTransition event = getStateTransition(PlaybackState.IDLE, PlayStateReason.NONE);
        assertThat(provider.fromTransition(event)).isEqualTo(STOP_REASON_CONCURRENT_STREAMING);
        assertThat(provider.fromTransition(event)).isEqualTo(STOP_REASON_PAUSE);
    }

    @NonNull
    private PlaybackStateTransition getStateTransition(PlaybackState state, PlayStateReason reason) {
        return new PlaybackStateTransition(state, reason, Urn.forTrack(123), 0, 0);
    }

}
