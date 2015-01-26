package com.soundcloud.android.tests.playlist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.NavigationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class PlaylistLikesTest extends ActivityTest<MainActivity> {

    public PlaylistLikesTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.likesUser.logIn(getInstrumentation().getTargetContext());
        setDependsOn(Flag.NEW_LIKES_END_TO_END);
        super.setUp();
    }

    public void testDrawerShowsPlaylists() {
        final PlaylistsScreen playlistsScreen = NavigationHelper.openLikedPlaylists(menuScreen);
        PlaylistDetailsScreen playlistDetailsScreen = playlistsScreen.clickPlaylistOnCurrentPageAt(0);
        assertEquals("Should go to Playlist screen", true, playlistDetailsScreen.isVisible());
    }

    public void testLoadsNextPage() {
        final PlaylistsScreen playlistsScreen = NavigationHelper.openLikedPlaylists(menuScreen);
        int numberOfTracks = playlistsScreen.getLoadedTrackCount();
        assertThat(numberOfTracks, is(greaterThan(0)));

        playlistsScreen.scrollToBottomOfTracksListAndLoadMoreItems();

        assertThat(playlistsScreen.getLoadedTrackCount(), is(greaterThan(numberOfTracks)));
    }

}
