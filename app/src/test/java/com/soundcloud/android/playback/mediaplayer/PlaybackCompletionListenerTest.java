package com.soundcloud.android.playback.mediaplayer;

import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;

import android.media.MediaPlayer;

public class PlaybackCompletionListenerTest extends AndroidUnitTest {

    private static int DURATION = 10000;

    private PlaybackCompletionListener playbackCompletionListener;

    @Mock private MediaPlayerAdapter mediaPlayerAdapter;
    @Mock private MediaPlayer mediaPlayer;

    @Before
    public void setUp() throws Exception {
        playbackCompletionListener = new PlaybackCompletionListener(mediaPlayerAdapter);
        when(mediaPlayer.getDuration()).thenReturn(DURATION);
        when(mediaPlayerAdapter.isSeekable()).thenReturn(true);
    }

    @Test
    public void shouldInvokeOnTrackEndInResetPositionInPlayingStateWithNoSeekPosition() {
        when(mediaPlayerAdapter.isPlayerPlaying()).thenReturn(true);
        playbackCompletionListener.onCompletion(mediaPlayer);
        verify(mediaPlayerAdapter).onPlaybackEnded();
    }

    @Test
    public void shouldInvokeOnTrackEndAtEndOfTrackInPlayingStateWithNoSeekPosition() {
        when(mediaPlayerAdapter.isPlayerPlaying()).thenReturn(true);
        when(mediaPlayer.getCurrentPosition()).thenReturn(DURATION);

        playbackCompletionListener.onCompletion(mediaPlayer);

        verify(mediaPlayerAdapter).onPlaybackEnded();
    }

    @Test
    public void shouldInvokeOnTrackEndAtEndOfTrackWithToleranceInPlayingStateWithNoSeekPosition() {
        when(mediaPlayerAdapter.isPlayerPlaying()).thenReturn(true);
        when(mediaPlayer.getCurrentPosition()).thenReturn(DURATION - PlaybackCompletionListener.COMPLETION_TOLERANCE_MS);

        playbackCompletionListener.onCompletion(mediaPlayer);

        verify(mediaPlayerAdapter).onPlaybackEnded();
    }

    @Test
    public void shouldInvokeRetryIfCurrentPositionOutsideTolerance() {
        final int resumeTime = DURATION - PlaybackCompletionListener.COMPLETION_TOLERANCE_MS - 1;
        when(mediaPlayerAdapter.isPlayerPlaying()).thenReturn(true);
        when(mediaPlayer.getCurrentPosition()).thenReturn(resumeTime);
        playbackCompletionListener.onCompletion(mediaPlayer);

        verify(mediaPlayerAdapter).setResumeTimeAndInvokeErrorListener(same(mediaPlayer), Matchers.eq((long) resumeTime));
        verify(mediaPlayerAdapter, never()).onPlaybackEnded();
    }

    @Test
    public void shouldInvokeStopInErrorState() {
        when(mediaPlayerAdapter.isPlayerPlaying()).thenReturn(false);
        when(mediaPlayerAdapter.isInErrorState()).thenReturn(true);
        when(mediaPlayer.getCurrentPosition()).thenReturn(DURATION);

        playbackCompletionListener.onCompletion(mediaPlayer);
        verify(mediaPlayerAdapter).stop(mediaPlayer);
    }
}
