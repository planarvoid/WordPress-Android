package com.soundcloud.android.playback.ui.view;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.offline.OfflinePlaybackOperations.TrackNotAvailableOffline;
import static com.soundcloud.android.playback.PlaybackOperations.UnskippablePeriodException;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowToast;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class PlaybackToastHelperTest {

    private PlaybackToastHelper toastHelper;

    @Mock private PlaySessionStateProvider playSessionStateProvider;

    @Before
    public void setUp() throws Exception {
        toastHelper = new PlaybackToastHelper(Robolectric.application, playSessionStateProvider);
    }

    @Test
    public void showsAdInProgressIfIsPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(true);

        toastHelper.showUnskippableAdToast();

        expect(ShadowToast.getLatestToast()).toHaveMessage(R.string.ad_in_progress);
    }

    @Test
    public void showsResumePlayingIfIsNotPlaying() {
        when(playSessionStateProvider.isPlaying()).thenReturn(false);

        toastHelper.showUnskippableAdToast();

        expect(ShadowToast.getLatestToast()).toHaveMessage(R.string.ad_resume_playing_to_continue);
    }

    @Test
    public void showsTrackNotAvailableOfflineOnPlaybackError() {
        toastHelper.showToastOnPlaybackError(new TrackNotAvailableOffline());

        expect(ShadowToast.getLatestToast()).toHaveMessage(R.string.offline_track_not_available);
    }

    @Test
    public void showAdToastOnPlaybackError() {
        toastHelper.showToastOnPlaybackError(new UnskippablePeriodException());

        expect(ShadowToast.getLatestToast()).toHaveMessage(R.string.ad_resume_playing_to_continue);
    }
}