package com.soundcloud.android.cast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayStatePublisher;
import com.soundcloud.android.playback.PlayStateReason;
import com.soundcloud.android.playback.PlaybackItem;
import com.soundcloud.android.playback.PlaybackState;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.CurrentDateProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.util.List;

public class CastPlayStateReporterTest extends AndroidUnitTest {

    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final long fakeProgress = 123L;
    private static final long fakeDuration = 456L;

    private CastPlayStateReporter castPlayStateReporter;

    @Mock private PlayStatePublisher playStatePublisher;
    @Mock private CurrentDateProvider dateProvider;
    @Mock private CastPlayStateReporter.Listener listener;

    @Captor private ArgumentCaptor<PlaybackStateTransition> playbackStateTransitionArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        when(dateProvider.getCurrentTime()).thenReturn(98461321L);
        castPlayStateReporter = new CastPlayStateReporter(playStatePublisher, dateProvider);
    }

    @Test
    public void listenerIsCalledWhenStateChanges() {
        castPlayStateReporter.setListener(listener);

        castPlayStateReporter.reportPlaying(TRACK_URN, fakeProgress, fakeDuration);

        verify(listener).onStateChangePublished(playbackStateTransitionArgumentCaptor.capture());

        PlaybackStateTransition stateTransition = playbackStateTransitionArgumentCaptor.getValue();
        assertThat(stateTransition.getProgress().getPosition()).isEqualTo(fakeProgress);
        assertThat(stateTransition.getProgress().getDuration()).isEqualTo(fakeDuration);
        assertThat(stateTransition.getNewState()).isEqualTo(PlaybackState.PLAYING);
        assertThat(stateTransition.getUrn()).isEqualTo(TRACK_URN);
    }

    @Test
    public void reportDisconnectionForwardsItAsIdleStateChange() {
        castPlayStateReporter.reportDisconnection(TRACK_URN, fakeProgress, fakeDuration);

        assertPlaybackStateTransition(PlaybackState.IDLE, PlayStateReason.CAST_DISCONNECTED, TRACK_URN, fakeProgress, fakeDuration);
    }

    @Test
    public void reportPlayingForwardsItAsStateChange() {
        castPlayStateReporter.reportPlaying(TRACK_URN, fakeProgress, fakeDuration);

        assertPlaybackStateTransition(PlaybackState.PLAYING, PlayStateReason.NONE, TRACK_URN, fakeProgress, fakeDuration);
    }

    @Test
    public void reportPlayingResetForwardsItAsBufferingStateChangeByZeroingProgressAndDuration() {
        castPlayStateReporter.reportPlayingReset(TRACK_URN);

        assertPlaybackStateTransition(PlaybackState.BUFFERING, PlayStateReason.NONE, TRACK_URN, 0, 0);
    }

    @Test
    public void reportPlayingErrorForwardsItAsIdleStateChangeByZeroingProgressAndDuration() {
        castPlayStateReporter.reportPlayingError(TRACK_URN);

        assertPlaybackStateTransition(PlaybackState.IDLE, PlayStateReason.ERROR_FAILED, TRACK_URN, 0, 0);
    }

    @Test
    public void reportPausedForwardsItAsStateChange() {
        castPlayStateReporter.reportPaused(TRACK_URN, fakeProgress, fakeDuration);

        assertPlaybackStateTransition(PlaybackState.IDLE, PlayStateReason.NONE, TRACK_URN, fakeProgress, fakeDuration);
    }

    @Test
    public void reportBufferingForwardsItAsStateChange() {
        castPlayStateReporter.reportBuffering(TRACK_URN, fakeProgress, fakeDuration);

        assertPlaybackStateTransition(PlaybackState.BUFFERING, PlayStateReason.NONE, TRACK_URN, fakeProgress, fakeDuration);
    }

    private void assertPlaybackStateTransition(PlaybackState state, PlayStateReason reason, Urn urn, long progress, long duration) {
        PlaybackStateTransition stateTransition = captureLastStateTransition();
        assertThat(stateTransition.getNewState()).isSameAs(state);
        assertThat(stateTransition.getReason()).isSameAs(reason);
        assertThat(stateTransition.getUrn()).isEqualTo(urn);
        assertThat(stateTransition.getProgress().getPosition()).isEqualTo(progress);
        assertThat(stateTransition.getProgress().getDuration()).isEqualTo(duration);
    }

    private PlaybackStateTransition captureLastStateTransition() {
        verify(playStatePublisher, atLeastOnce()).publish(playbackStateTransitionArgumentCaptor.capture(), any(PlaybackItem.class), eq(false));
        final List<PlaybackStateTransition> values = playbackStateTransitionArgumentCaptor.getAllValues();
        return values.isEmpty() ? null : values.get(values.size() - 1);
    }

}