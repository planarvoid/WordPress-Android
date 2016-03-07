package com.soundcloud.android.playback.ui.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.shadows.ShadowToast;

public class PlaybackToastHelperTest extends AndroidUnitTest {

    private PlaybackToastHelper toastHelper;

    @Mock private PlaySessionStateProvider playSessionStateProvider;

    @Before
    public void setUp() throws Exception {
        toastHelper = new PlaybackToastHelper(context(), playSessionStateProvider);
    }

    @Test
    public void showsAdInProgressIfIsPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        toastHelper.showToastOnPlaybackError(PlaybackResult.ErrorReason.UNSKIPPABLE);

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(resources().getString(R.string.ads_ad_in_progress));
    }

    @Test
    public void showsResumePlayingIfIsNotPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(false);

        toastHelper.showToastOnPlaybackError(PlaybackResult.ErrorReason.UNSKIPPABLE);

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(resources().getString(R.string.ads_resume_playing_ad_to_continue));
    }

    @Test
    public void showsTrackNotAvailableOffline() {
        toastHelper.showToastOnPlaybackError(PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_OFFLINE);

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(resources().getString(R.string.offline_track_not_available));
    }

    @Test
    public void showsMissingPlayableTracks() {
        toastHelper.showToastOnPlaybackError(PlaybackResult.ErrorReason.MISSING_PLAYABLE_TRACKS);

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(resources().getString(R.string.playback_missing_playable_tracks));
    }

    @Test
    public void showsUnableToCastTrack() {
        toastHelper.showToastOnPlaybackError(PlaybackResult.ErrorReason.TRACK_UNAVAILABLE_CAST);

        assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(resources().getString(R.string.cast_unable_play_track));
    }
}
