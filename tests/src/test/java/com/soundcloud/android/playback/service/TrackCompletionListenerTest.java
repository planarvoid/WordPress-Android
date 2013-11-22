package com.soundcloud.android.playback.service;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.media.MediaPlayer;

@RunWith(SoundCloudTestRunner.class)
public class TrackCompletionListenerTest {


    private TrackCompletionListener trackCompletionListener;

    @Mock
    PlaybackService playbackService;
    @Mock
    PlayQueue playQueue;
    @Mock
    MediaPlayer mediaPlayer;

    private static final long TRACK_ID = 123L;
    private static int DURATION = 10000;

    @Before
    public void setUp() throws Exception {
        trackCompletionListener = new TrackCompletionListener(playbackService);
        when(playbackService.getDuration()).thenReturn(DURATION);
        when(playbackService._isSeekable()).thenReturn(true);
        when(playbackService.getPlayQueueInternal()).thenReturn(playQueue);
        when(playQueue.getCurrentTrackId()).thenReturn(TRACK_ID);
    }

    @Test
    public void shouldInvokeOnTrackEndInResetPositionInPlayingStateWithNoSeekPosition() {
        when(playbackService.getPlaybackStateInternal()).thenReturn(PlaybackState.PLAYING);

        trackCompletionListener.onCompletion(mediaPlayer);

        verify(playbackService).onTrackEnded();
    }

    @Test
    public void shouldInvokeOnTrackEndAtEndOfTrackInPlayingStateWithNoSeekPosition() {
        when(playbackService.getPlaybackStateInternal()).thenReturn(PlaybackState.PLAYING);
        when(mediaPlayer.getCurrentPosition()).thenReturn(DURATION);

        trackCompletionListener.onCompletion(mediaPlayer);

        verify(playbackService).onTrackEnded();
    }

    @Test
    public void shouldInvokeOnTrackEndAtEndOfTrackWithToleranceInPlayingStateWithNoSeekPosition() {
        when(playbackService.getPlaybackStateInternal()).thenReturn(PlaybackState.PLAYING);
        when(mediaPlayer.getCurrentPosition()).thenReturn(DURATION - TrackCompletionListener.COMPLETION_TOLERANCE_MS);

        trackCompletionListener.onCompletion(mediaPlayer);

        verify(playbackService).onTrackEnded();
    }

    @Test
    public void shouldInvokeRetryIfCurrentPositionOutsideTolerance() {
        final int resumeTime = DURATION - TrackCompletionListener.COMPLETION_TOLERANCE_MS - 1;
        when(playbackService.getPlaybackStateInternal()).thenReturn(PlaybackState.PLAYING);
        when(mediaPlayer.getCurrentPosition()).thenReturn(resumeTime);
        trackCompletionListener.onCompletion(mediaPlayer);

        verify(playbackService).setResumeTimeAndInvokeErrorListener(same(mediaPlayer), eq(new ResumeInfo(TRACK_ID, resumeTime)));
        verify(playbackService, never()).onTrackEnded();
    }

    @Test
    public void shouldInvokeStopInErrorState() {
        when(playbackService.getPlaybackStateInternal()).thenReturn(PlaybackState.ERROR);
        when(mediaPlayer.getCurrentPosition()).thenReturn(DURATION);

        trackCompletionListener.onCompletion(mediaPlayer);

        verify(playbackService).stop();
    }

}
