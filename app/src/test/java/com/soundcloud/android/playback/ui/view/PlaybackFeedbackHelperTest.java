package com.soundcloud.android.playback.ui.view;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.feedback.Feedback;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.snackbar.FeedbackController;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PlaybackFeedbackHelperTest extends AndroidUnitTest {

    private PlaybackFeedbackHelper feedbackHelper;

    @Mock private PlaySessionStateProvider playSessionStateProvider;
    @Mock private FeedbackController feedbackController;

    @Before
    public void setUp() throws Exception {
        feedbackHelper = new PlaybackFeedbackHelper(playSessionStateProvider, feedbackController);
    }

    @Test
    public void showsAdInProgressIfIsPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        feedbackHelper.showFeedbackOnPlaybackError(PlaybackResult.ErrorReason.UNSKIPPABLE);

        verify(feedbackController).showFeedback(Feedback.create(R.string.ads_ad_in_progress));
    }

    @Test
    public void showsResumePlayingIfIsNotPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(false);

        feedbackHelper.showFeedbackOnPlaybackError(PlaybackResult.ErrorReason.UNSKIPPABLE);

        verify(feedbackController).showFeedback(Feedback.create(R.string.ads_resume_playing_ad_to_continue));
    }

    @Test
    public void showsTrackNotAvailableOffline() {
        feedbackHelper.showFeedbackOnPlaybackError(PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_OFFLINE);

        verify(feedbackController).showFeedback(Feedback.create(R.string.offline_track_not_available));
    }

    @Test
    public void showsMissingPlayableTracks() {
        feedbackHelper.showFeedbackOnPlaybackError(PlaybackResult.ErrorReason.MISSING_PLAYABLE_TRACKS);

        verify(feedbackController).showFeedback(Feedback.create(R.string.playback_missing_playable_tracks));
    }

    @Test
    public void showsUnableToCastTrack() {
        feedbackHelper.showFeedbackOnPlaybackError(PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_CAST);

        verify(feedbackController).showFeedback(Feedback.create(R.string.cast_unable_play_track));
    }
}
