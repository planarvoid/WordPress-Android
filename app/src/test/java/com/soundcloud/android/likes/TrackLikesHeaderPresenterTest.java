package com.soundcloud.android.likes;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.TextView;


@RunWith(SoundCloudTestRunner.class)
public class TrackLikesHeaderPresenterTest {

    private TrackLikesHeaderPresenter presenter;

    @Mock private Fragment fragment;
    @Mock private PlaybackOperations playbackOperations;

    @Before
    public void setUp() throws Exception {
        presenter = new TrackLikesHeaderPresenter();
        View view = mock(View.class);
        when(view.getContext()).thenReturn(Robolectric.application);
        presenter.onViewCreated(fragment, view, null);
    }

    @Test
    public void hideShuffleButtonForZeroLikedTracks() {
        presenter.updateTrackCount(0);
        View shuffleButton = getShuffleButton();
        expect(shuffleButton).toBeGone();
    }

    @Test
    public void disableShuffleButtonForZeroLikedTracks() {
        presenter.updateTrackCount(0);
        expect(getShuffleButton()).not.toBeEnabled();
    }

    @Test
    public void hideShuffleButtonForOneLikedTrack() {
        presenter.updateTrackCount(1);
        expect(getShuffleButton()).toBeGone();
    }

    @Test
    public void disableShuffleButtonForOneTrackLike() {
        presenter.updateTrackCount(1);
        expect(getShuffleButton()).not.toBeEnabled();
    }

    @Test
    public void displayShuffleButtonForMoreThanOneLikedTracks() {
        presenter.updateTrackCount(2);
        expect(getShuffleButton()).toBeVisible();
    }

    @Test
    public void enableShuffleButtonForMoreThanOneLikedTracks() {
        presenter.updateTrackCount(2);
        expect(getShuffleButton()).toBeEnabled();
    }

    @Test
    public void showNumberOfLikedTracksForZeroLikedTracks() {
        presenter.updateTrackCount(0);
        expect(getHeaderText().getText().toString()).toEqual(Robolectric.application.getResources()
                .getString(R.string.number_of_liked_tracks_you_liked_zero));
    }

    @Test
    public void showNumberOfLikedTracksForMoreThanZeroLikedTracks() {
        presenter.updateTrackCount(1);
        expect(getHeaderText().getText().toString()).toEqual(Robolectric.application.getResources()
                .getQuantityString(R.plurals.number_of_liked_tracks_you_liked, 1, 1));
    }

    @Test
    public void doNotUpdateTextWhenUpdatingTrackCountOnSyncingState() {
        presenter.showSyncingState();
        presenter.updateTrackCount(4);
        expect(getHeaderText().getText().toString()).toEqual(Robolectric.application.getResources()
                .getString(R.string.offline_update_in_progress));
    }

    private View getShuffleButton() {
        return presenter.getHeaderView().findViewById(R.id.shuffle_btn);
    }

    private TextView getHeaderText() {
        return (TextView) presenter.getHeaderView().findViewById(R.id.header_text);
    }

}