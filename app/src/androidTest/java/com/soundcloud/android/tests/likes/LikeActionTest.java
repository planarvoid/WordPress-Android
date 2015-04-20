package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.helpers.TrackItemElementHelper.assertLikeActionOnUnlikedTrack;
import static com.soundcloud.android.framework.helpers.PlaylistDetailsScreenHelper.assertLikeActionOnUnlikedPlaylist;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
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

    public void testLikedTrackAddedToLikeCollectionWhenLikingFromTrackItemOverflowMenu() throws Exception {
        final MainScreen mainScreen = new MainScreen(solo);
        mainScreen
                .actionBar()
                .clickSearchButton();

        final TrackItemElement expectedTrack = mainScreen
                .actionBar()
                .doSearch("Acceptance")
                .touchTracksTab()
                .getTracks()
                .get(0);

        networkManager.switchWifiOff();
        assertLikeActionOnUnlikedTrack(this, expectedTrack);
        final String expectedTitle = expectedTrack.getTitle();

        solo.goBack();
        solo.goBack();

        final String actualTitle = menuScreen
                .open()
                .clickLikes()
                .tracks()
                .get(0)
                .getTitle();

        assertEquals("The track we liked from the search page should be the same as the top track in your likes",
                expectedTitle, actualTitle);
    }

    public void testLikedPlaylistAddedToLikeCollectionWhenLikingFromPlaylistScreenEngagementBar() throws Exception {
        final MainScreen mainScreen = new MainScreen(solo);
        mainScreen
                .actionBar()
                .clickSearchButton();

        final PlaylistDetailsScreen playlistScreen = mainScreen
                .actionBar()
                .doSearch("Acceptance")
                .touchPlaylistsTab()
                .getPlaylists()
                .get(0)
                .click();

        final String expectedTitle = playlistScreen.getTitle();

        networkManager.switchWifiOff();
        assertLikeActionOnUnlikedPlaylist(playlistScreen);

        solo.goBack();
        solo.goBack();
        solo.goBack();
        solo.goBack();

        final String actualTitle = menuScreen
                .open()
                .clickPlaylist()
                .touchLikedPlaylistsTab()
                .get(0)
                .getTitle();

        assertEquals("The playlist we liked from the playlist detail screen should be the same as the top playlist in "+
                "your liked playlists", expectedTitle, actualTitle);
    }
}
