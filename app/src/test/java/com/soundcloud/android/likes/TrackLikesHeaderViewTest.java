package com.soundcloud.android.likes;

import static org.assertj.android.api.Assertions.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.offline.DownloadableHeaderView;
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

    @Before
    public void setUp() throws Exception {
        trackLikesHeaderView = new TrackLikesHeaderView(resources(), new DownloadableHeaderView(resources()));
        trackLikesHeaderView.onViewCreated(View.inflate(context(), R.layout.track_likes_header, null));
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
                .getQuantityString(R.plurals.number_of_liked_tracks_you_liked, 1, 1));
    }

    public void displayOverflowMenuWhenOfflineSyncOptionIsEnabled() {
        trackLikesHeaderView.updateOverflowMenuButton(true);
        assertThat(getOverflowMenuButton()).isVisible();
    }

    public void displayOverflowMenuWhenOfflineSyncOptionIsDisabled() {
        trackLikesHeaderView.updateOverflowMenuButton(false);
        assertThat(getOverflowMenuButton()).isInvisible();
    }

    private View getShuffleButton() {
        return trackLikesHeaderView.getHeaderView().findViewById(R.id.shuffle_btn);
    }

    private View getOverflowMenuButton() {
        return trackLikesHeaderView.getHeaderView().findViewById(R.id.overflow_button);
    }

    private TextView getHeaderText() {
        return (TextView) trackLikesHeaderView.getHeaderView().findViewById(R.id.header_text);
    }

}
