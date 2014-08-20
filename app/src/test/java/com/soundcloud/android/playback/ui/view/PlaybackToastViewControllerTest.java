package com.soundcloud.android.playback.ui.view;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.playback.service.Playa;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowToast;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackToastViewControllerTest {

    private PlaybackToastViewController controller;

    @Mock private PlaySessionStateProvider playSessionStateProvider;

    @Before
    public void setUp() throws Exception {
        controller = new PlaybackToastViewController(Robolectric.application, playSessionStateProvider);
    }

    @Test
    public void showAdInProgressIfIsPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        controller.showUnkippableAdToast();

        expect(ShadowToast.getLatestToast()).toHaveMessage(R.string.ad_in_progress);
    }

    @Test
    public void showResumePlayingIfIsNotPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(false);

        controller.showUnkippableAdToast();

        expect(ShadowToast.getLatestToast()).toHaveMessage(R.string.ad_resume_playing_to_continue);
    }

    @Test
    public void showErrorWithReasonNotFoundShowsUnableToPlayError() {
        controller.showError(Playa.Reason.ERROR_NOT_FOUND);

        expect(ShadowToast.getLatestToast()).toHaveMessage(R.string.playback_error_unable_to_play);
    }

    @Test
    public void showErrorWithReasonForbiddenShowsUnableToPlayError() {
        controller.showError(Playa.Reason.ERROR_FORBIDDEN);

        expect(ShadowToast.getLatestToast()).toHaveMessage(R.string.playback_error_unable_to_play);
    }

    @Test
    public void showErrorWithReasonFailedShowsConnectionError() {
        controller.showError(Playa.Reason.ERROR_FAILED);

        expect(ShadowToast.getLatestToast()).toHaveMessage(R.string.playback_error_connection);
    }
}