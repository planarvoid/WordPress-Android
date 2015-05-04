package com.soundcloud.android.tests.playlist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class PlaylistLikesTest extends ActivityTest<MainActivity> {

    public PlaylistLikesTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.playlistLikesUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testDrawerShowsPlaylists() {
        PlaylistsScreen playlistsScreen = menuScreen.open().clickPlaylist();
        playlistsScreen.touchLikedPlaylistsTab();
        if(!playlistsScreen.hasLikes()) {
            playlistsScreen.pullToRefresh();
        }
        PlaylistDetailsScreen playlistDetailsScreen = playlistsScreen.clickPlaylistOnCurrentPageAt(0);
        assertEquals("Should go to Playlist screen", true, playlistDetailsScreen.isVisible());
    }

    public void testLoadsNextPage() {
        PlaylistsScreen playlistsScreen = menuScreen.open().clickPlaylist();
        playlistsScreen.touchLikedPlaylistsTab();
        if(!playlistsScreen.hasLikes()) {
            playlistsScreen.pullToRefresh();
        }
        int numberOfTracks = playlistsScreen.getLoadedTrackCount();
        assertThat(numberOfTracks, is(greaterThan(0)));

        playlistsScreen.scrollToBottomOfTracksListAndLoadMoreItems();

        assertThat(playlistsScreen.getLoadedTrackCount(), is(greaterThan(numberOfTracks)));
    }
}
