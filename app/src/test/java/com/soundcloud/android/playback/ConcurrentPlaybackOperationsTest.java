package com.soundcloud.android.playback;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ConcurrentPlaybackOperationsTest extends AndroidUnitTest {

    @Mock StopReasonProvider stopReasonProvider;
    @Mock PlaySessionStateProvider playSessionStateProvider;
    @Mock PlaySessionController playSessionController;
    @Mock PlaybackToastHelper playbackToastHelper;

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
    public void pauseIfPlayingShowsToastWhenIsPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        operations.pauseIfPlaying();

        verify(playbackToastHelper).showConcurrentStreamingStoppedToast();
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
    public void pauseIfPlayingDoesNotShowToastIfNotPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(false);

        operations.pauseIfPlaying();

        verify(playbackToastHelper, never()).showConcurrentStreamingStoppedToast();
    }

}
