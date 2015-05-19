package com.soundcloud.android.tests.playlist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class PlaylistPostsTest extends ActivityTest<MainActivity> {

    public PlaylistPostsTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.playlistUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testDrawerShowsPlaylists() {
        PlaylistsScreen playlistsScreen = menuScreen.open().clickPlaylists();
        playlistsScreen.touchPostedPlaylistsTab();
        PlaylistDetailsScreen playlistDetailsScreen = playlistsScreen.clickPlaylistOnCurrentPageAt(0);
        assertEquals("Should go to Playlist screen", true, playlistDetailsScreen.isVisible());
    }

    public void testLoadsNextPage() {
        PlaylistsScreen playlistsScreen = menuScreen.open().clickPlaylists();
        playlistsScreen.touchPostedPlaylistsTab();
        int numberOfTracks = playlistsScreen.getLoadedTrackCount();
        assertThat(numberOfTracks, is(greaterThan(0)));

        playlistsScreen.scrollToBottomOfTracksListAndLoadMoreItems();

        assertThat(playlistsScreen.getLoadedTrackCount(), is(greaterThan(numberOfTracks)));
    }

}
