package com.soundcloud.android.playback;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConcurrentPlaybackOperationsTest {

    @Mock StopReasonProvider stopReasonProvider;
    @Mock PlaySessionStateProvider playSessionStateProvider;
    @Mock PlaySessionController playSessionController;
    @Mock CastConnectionHelper castConnectionHelper;
    @Mock PlaybackFeedbackHelper playbackFeedbackHelper;

    @InjectMocks private ConcurrentPlaybackOperations operations;

    @Test
    public void pauseIfPlayingSetsStopReasonWhenIsPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        operations.pauseIfPlaying();

        verify(stopReasonProvider).setPendingConcurrentPause();
    }

    @Test
    public void pauseIfPlayingFadesAndPausesWhenIsPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        operations.pauseIfPlaying();

        verify(playSessionController).fadeAndPause();
    }

    @Test
    public void pauseIfPlayingShowsFeedbackWhenIsPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        operations.pauseIfPlaying();

        verify(playbackFeedbackHelper).showConcurrentStreamingStoppedFeedback();
    }

    @Test
    public void pauseIfPlayingDoesNotSetStopReasonIfNotPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(false);

        operations.pauseIfPlaying();

        verify(stopReasonProvider, never()).setPendingConcurrentPause();
    }

    @Test
    public void pauseIfPlayingDoesNotPauseIfNotPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(false);

        operations.pauseIfPlaying();

        verify(playSessionController, never()).fadeAndPause();
    }

    @Test
    public void pauseIfPlayingDoesNotShowFeedbackIfNotPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(false);

        operations.pauseIfPlaying();

        verify(playbackFeedbackHelper, never()).showConcurrentStreamingStoppedFeedback();
    }

    @Test
    public void pauseIfPlayingDoesNotPauseWhileCastingSinceThePlaybackControlShouldBeOnTheReceiver() {
        when(castConnectionHelper.isCasting()).thenReturn(true);

        operations.pauseIfPlaying();

        verify(stopReasonProvider, never()).setPendingConcurrentPause();
        verify(playSessionController, never()).fadeAndPause();
        verify(playbackFeedbackHelper, never()).showConcurrentStreamingStoppedFeedback();
    }
}
