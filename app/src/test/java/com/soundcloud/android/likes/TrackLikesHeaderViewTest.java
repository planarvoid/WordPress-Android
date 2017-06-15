package com.soundcloud.android.likes;

import static org.assertj.android.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayKey;
import com.soundcloud.android.introductoryoverlay.IntroductoryOverlayPresenter;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.TextView;

public class TrackLikesHeaderViewTest extends AndroidUnitTest {

    private TrackLikesHeaderView trackLikesHeaderView;

    @Mock private Fragment fragment;
    @Mock private FragmentManager fragmentManager;
    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private TrackLikesHeaderView.Listener listener;
    @Mock private IntroductoryOverlayPresenter introductoryOverlayPresenter;

    @Before
    public void setUp() throws Exception {
        View view = View.inflate(context(), R.layout.track_likes_header, null);
        trackLikesHeaderView = new TrackLikesHeaderView(resources(),
                                                        introductoryOverlayPresenter,
                                                        view,
                                                        listener);
    }

    @Test
    public void hideShuffleButtonForZeroLikedTracks() {
        trackLikesHeaderView.updateTrackCount(0);
        assertThat(getShuffleButton()).isGone();
    }

    @Test
    public void disableShuffleButtonForZeroLikedTracks() {
        trackLikesHeaderView.updateTrackCount(0);
        assertThat(getShuffleButton()).isDisabled();
    }

    @Test
    public void hideShuffleButtonForOneLikedTrack() {
        trackLikesHeaderView.updateTrackCount(1);
        assertThat(getShuffleButton()).isGone();
    }

    @Test
    public void disableShuffleButtonForOneTrackLike() {
        trackLikesHeaderView.updateTrackCount(1);
        assertThat(getShuffleButton()).isDisabled();
    }

    @Test
    public void displayShuffleButtonForMoreThanOneLikedTracks() {
        trackLikesHeaderView.updateTrackCount(2);
        assertThat(getShuffleButton()).isVisible();
    }

    @Test
    public void enableShuffleButtonForMoreThanOneLikedTracks() {
        trackLikesHeaderView.updateTrackCount(2);
        assertThat(getShuffleButton()).isEnabled();
    }

    @Test
    public void hideHeaderViewForZeroLikedTracks() {
        trackLikesHeaderView.updateTrackCount(0);
        assertThat(trackLikesHeaderView.getHeaderView()).isGone();
    }

    @Test
    public void showNumberOfLikedTracksForMoreThanZeroLikedTracks() {
        trackLikesHeaderView.updateTrackCount(1);
        assertThat(getHeaderText()).hasText(RuntimeEnvironment.application.getResources()
                                                                          .getQuantityString(R.plurals.number_of_liked_tracks_you_liked,
                                                                                             1,
                                                                                             1));
    }

    @Test
    public void showIntroductoryOverlay() {
        trackLikesHeaderView.showOfflineIntroductoryOverlay();
        verify(introductoryOverlayPresenter).showIfNeeded(IntroductoryOverlayKey.LISTEN_OFFLINE_LIKES,
                                                                        getOfflineStateButton(),
                                                                        R.string.overlay_listen_offline_likes_title,
                                                                        R.string.overlay_listen_offline_likes_description);
    }

    private View getOfflineStateButton() {
        return trackLikesHeaderView.getHeaderView().findViewById(R.id.offline_state_button);
    }

    private View getShuffleButton() {
        return trackLikesHeaderView.getHeaderView().findViewById(R.id.shuffle_btn);
    }

    private TextView getHeaderText() {
        return (TextView) trackLikesHeaderView.getHeaderView().findViewById(R.id.header_text);
    }

}
