package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.tests.ActivityTest;

public class LikeActionTest extends ActivityTest<MainActivity> {

    public LikeActionTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.likesActionUser.logIn(getInstrumentation().getTargetContext());
    }

    // *** Ignore until we come up with a good way to prevent like actions from getting synced ***
    public void ignoreLikesSyncing_testLikedTrackAddedToLikeCollectionWhenLikingFromTrackItemOverflowMenu() throws Exception {
        final StreamScreen streamScreen = new StreamScreen(solo);

        final TrackItemElement expectedTrack = streamScreen
                .actionBar()
                .clickSearchButton()
                .actionBar()
                .doSearch("Acceptance")
                .touchTracksTab()
                .getTracks()
                .get(0);

        expectedTrack.clickOverflowButton().toggleLike();
        final String expectedTitle = expectedTrack.getTitle();

        solo.goBack();
        solo.goBack();
        assertThat("Stream should be visible", streamScreen, is(visible()));

        final String actualTitle = streamScreen
                .openMenu()
                .clickLikes()
                .tracks()
                .get(0)
                .getTitle();

        assertEquals("The track we liked from the search page should be the same as the top track in your likes",
                expectedTitle, actualTitle);
    }

    // *** Ignore until we come up with a good way to prevent like actions from getting synced ***
    public void ignoreLikesSyncing_testLikedPlaylistAddedToLikeCollectionWhenLikingFromPlaylistScreenEngagementBar() throws Exception {
        final StreamScreen streamScreen = new StreamScreen(solo);

        final PlaylistDetailsScreen playlistDetailsScreen = streamScreen
                .actionBar()
                .clickSearchButton()
                .actionBar()
                .doSearch("Acceptance")
                .touchPlaylistsTab()
                .getPlaylists()
                .get(0)
                .click();

        final String expectedTitle = playlistDetailsScreen.getTitle();

        playlistDetailsScreen.touchToggleLike();

        solo.goBack();
        solo.goBack();
        solo.goBack();
        solo.goBack();
        assertThat("Stream should be visible", streamScreen, is(visible()));

        final String actualTitle = streamScreen
                .openMenu()
                .clickPlaylists()
                .touchLikedPlaylistsTab()
                .get(0)
                .getTitle();

        assertEquals("The playlist we liked from the playlist detail screen should be the same as the top playlist in "+
                "your liked playlists", expectedTitle, actualTitle);
    }
}
