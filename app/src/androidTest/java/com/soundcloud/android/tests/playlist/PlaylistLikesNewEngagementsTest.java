package com.soundcloud.android.tests.playlist;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
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
        setDependsOn(Flag.NEW_PLAYLIST_ENGAGEMENTS);
        TestUser.likesUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
    }

    // Given I liked a playlist
    // Given I go the playlists screen
    // Then the playlists should the first one
    public void testLastLikedPlaylistShouldAppearOnTop() {
        networkManager.switchWifiOff();

        final PlaylistsScreen playlistsScreen = menuScreen.open().clickPlaylist();
        waiter.waitForContentAndRetryIfLoadingFailed();
        final String expectedTitle = playlistsScreen.get(0).getTitle();
        likePlaylistAt(playlistsScreen, 0);

        playlistsScreen.touchLikedPlaylistsTab();

        assertEquals(expectedTitle, playlistsScreen.get(0).getTitle());

        networkManager.switchWifiOn();
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

    public void testLikingAndUnlikingPlaylistFromOverflowMenu() {
        networkManager.switchWifiOff();

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
        assertThat(playlistsScreen.hasLikes(), is(false));

        playlistsScreen.touchPostedPlaylistsTab();

        assertThat(openOverflowMenu(playlistsScreen).isLiked(), is(false));

        networkManager.switchWifiOn();
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
}
