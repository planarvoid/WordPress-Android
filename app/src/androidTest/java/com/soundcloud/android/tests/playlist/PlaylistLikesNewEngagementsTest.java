package com.soundcloud.android.tests.playlist;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.NavigationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.screens.elements.PlaylistItemOverflowMenu;
import com.soundcloud.android.tests.ActivityTest;

public class PlaylistLikesNewEngagementsTest extends ActivityTest<MainActivity> {

    public PlaylistLikesNewEngagementsTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.likesUser.logIn(getInstrumentation().getTargetContext());
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

    // Given I liked a playlist
    // Given I go the playlists screen
    // Then the playlists should the first one
    public void testLastLikedPlaylistShouldAppearOnTop() {
        final PlaylistsScreen playlistsScreen = menuScreen.open().clickPlaylist();
        waiter.waitForContentAndRetryIfLoadingFailed();
        final String expectedTitle = playlistsScreen.get(0).getTitle();
        likePlaylistAt(playlistsScreen, 0);

        playlistsScreen.touchLikedPlaylistsTab();

        assertEquals(expectedTitle, playlistsScreen.get(0).getTitle());
    }

    public void testLikingAndUnlikingPlaylistFromOverflowMenu() {
        final PlaylistsScreen playlistsScreen = menuScreen.open().clickPlaylist();
        final String expectedTitle = playlistsScreen.get(0).getTitle();

        likePlaylist(openOverflowMenu(playlistsScreen));
        playlistsScreen.touchLikedPlaylistsTab();
        assertFirstItemTitle(playlistsScreen, expectedTitle);

        // assert liked + then unlike
        final PlaylistItemOverflowMenu overflowMenu = openOverflowMenu(playlistsScreen);
        assertTrue(overflowMenu.isLiked());
        overflowMenu.toggleLike();

        // assert item now gone
        assertThat(expectedTitle, not(equalTo(playlistsScreen.get(0).getTitle())));

        playlistsScreen.touchPostedPlaylistsTab();

        assertFalse(openOverflowMenu(playlistsScreen).isLiked());
    }

    private PlaylistItemOverflowMenu openOverflowMenu(PlaylistsScreen playlistsScreen) {
        return playlistsScreen.get(0).clickOverflow();
    }

    private void assertFirstItemTitle(PlaylistsScreen playlistsScreen, String expectedTitle) {
        assertThat(expectedTitle, equalTo(playlistsScreen.get(0).getTitle()));
    }

    private void likePlaylist(PlaylistItemOverflowMenu playlistItemOverflowMenu) {
        if (playlistItemOverflowMenu.isLiked()) {
            playlistItemOverflowMenu.toggleLike();
        }
        playlistItemOverflowMenu.toggleLike();
    }

    private void likePlaylistAt(PlaylistsScreen playlistsScreen, int index) {
        final PlaylistDetailsScreen playlistDetailsScreen = playlistsScreen.clickPlaylistAt(index);
        unlikePlaylistIfLiked(playlistDetailsScreen);
        playlistDetailsScreen.touchToggleLike();
        playlistDetailsScreen.clickBack();
    }

    private void unlikePlaylistIfLiked(PlaylistDetailsScreen playlistDetailsScreen) {
        if (playlistDetailsScreen.isLiked()) {
            playlistDetailsScreen.touchToggleLike();
        }
    }

}
