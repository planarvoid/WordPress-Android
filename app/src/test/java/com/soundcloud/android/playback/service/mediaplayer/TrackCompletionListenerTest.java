package com.soundcloud.android.playback.service.mediaplayer;

import static com.soundcloud.android.playback.service.Playa.PlayaState;
import static com.soundcloud.android.playback.service.Playa.Reason;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;

import android.media.MediaPlayer;
import android.os.Build;

@RunWith(SoundCloudTestRunner.class)
public class TrackCompletionListenerTest {

    private TrackCompletionListener trackCompletionListener;

    @Mock
    MediaPlayerAdapter mediaPlayerAdapter;
    @Mock
    MediaPlayer mediaPlayer;

    private static final long TRACK_ID = 123L;
    private static int DURATION = 10000;

    @Before
    public void setUp() throws Exception {
        // Default to JellyBean for normal functionality
        TestHelper.setSdkVersion(Build.VERSION_CODES.JELLY_BEAN);

        trackCompletionListener = new TrackCompletionListener(mediaPlayerAdapter);
        when(mediaPlayer.getDuration()).thenReturn(DURATION);
        when(mediaPlayerAdapter.isSeekable()).thenReturn(true);
    }

    @Test
    public void shouldInvokeOnTrackEndInResetPositionInPlayingStateWithNoSeekPosition() {
        when(mediaPlayerAdapter.getLastStateTransition()).thenReturn(new Playa.StateTransition(PlayaState.PLAYING, Reason.NONE));
        when(mediaPlayerAdapter.getState()).thenReturn(PlayaState.PLAYING);
        trackCompletionListener.onCompletion(mediaPlayer);

        verify(mediaPlayerAdapter).onTrackEnded();
    }

    @Test
    public void shouldInvokeOnTrackEndAtEndOfTrackInPlayingStateWithNoSeekPosition() {
        when(mediaPlayerAdapter.getLastStateTransition()).thenReturn(new Playa.StateTransition(PlayaState.PLAYING, Reason.NONE));
        when(mediaPlayer.getCurrentPosition()).thenReturn(DURATION);

        trackCompletionListener.onCompletion(mediaPlayer);

        verify(mediaPlayerAdapter).onTrackEnded();
    }

    @Test
    public void shouldInvokeOnTrackEndAtEndOfTrackWithToleranceInPlayingStateWithNoSeekPosition() {
        when(mediaPlayerAdapter.getLastStateTransition()).thenReturn(new Playa.StateTransition(PlayaState.PLAYING, Reason.NONE));
        when(mediaPlayer.getCurrentPosition()).thenReturn(DURATION - TrackCompletionListener.COMPLETION_TOLERANCE_MS);

        trackCompletionListener.onCompletion(mediaPlayer);

        verify(mediaPlayerAdapter).onTrackEnded();
    }

    @Test
    public void shouldInvokeRetryIfCurrentPositionOutsideTolerance() {
        final int resumeTime = DURATION - TrackCompletionListener.COMPLETION_TOLERANCE_MS - 1;
        when(mediaPlayerAdapter.getState()).thenReturn(PlayaState.PLAYING);
        when(mediaPlayer.getCurrentPosition()).thenReturn(resumeTime);
        trackCompletionListener.onCompletion(mediaPlayer);

        verify(mediaPlayerAdapter).setResumeTimeAndInvokeErrorListener(same(mediaPlayer), Matchers.eq((long) resumeTime));
        verify(mediaPlayerAdapter, never()).onTrackEnded();
    }

    @Test
    public void shouldInvokeOnCompleteIfBuildAfterJellyBeanAndNotSeekingOrResuming() {
        TestHelper.setSdkVersion(Build.VERSION_CODES.JELLY_BEAN + 1);
        when(mediaPlayerAdapter.getLastStateTransition()).thenReturn(new Playa.StateTransition(PlayaState.PLAYING, Reason.NONE));
        trackCompletionListener.onCompletion(mediaPlayer);
        verify(mediaPlayerAdapter).onTrackEnded();
    }

    @Test
    public void shouldInvokeStopInErrorState() {
        when(mediaPlayerAdapter.getLastStateTransition()).thenReturn(new Playa.StateTransition(PlayaState.IDLE, Reason.ERROR_FAILED));
        when(mediaPlayer.getCurrentPosition()).thenReturn(DURATION);

        trackCompletionListener.onCompletion(mediaPlayer);
        verify(mediaPlayerAdapter).stop(mediaPlayer);
    }

}
