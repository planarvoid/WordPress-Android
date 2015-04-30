package com.soundcloud.android.tests.playlist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class PlaylistLikesNewEngagementsTest extends ActivityTest<MainActivity> {
    private PlaylistsScreen playlistsScreen;

    public PlaylistLikesNewEngagementsTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.likesUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        playlistsScreen = menuScreen.open().clickPlaylist();
    }

    // *** Ignore until we come up with a good way to prevent like actions from getting synced ***
    // Given I liked a playlist
    // Given I go the liked playlists tab on the playlists screen
    // Then the playlist should the first one
    public void ignoreLikesSyncing_testLastLikedPlaylistShouldAppearOnTop() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        final String expectedTitle = playlistsScreen.get(0).getTitle();
        playlistsScreen.get(0).clickOverflow().toggleLike();

        playlistsScreen.touchLikedPlaylistsTab();

        assertEquals(expectedTitle, playlistsScreen.get(0).getTitle());
        playlistsScreen.get(0).clickOverflow().toggleLike();
    }

    // *** Ignore until we come up with a good way to prevent like actions from getting synced ***
    public void ignoreLikesSyncing_testLikingAndUnlikingPlaylistFromOverflowMenu() {
        // assert liked
        final String expectedTitle = playlistsScreen.get(0).getTitle();
        playlistsScreen.get(0).clickOverflow().toggleLike();

        playlistsScreen.touchLikedPlaylistsTab();
        assertEquals(expectedTitle, playlistsScreen.get(0).getTitle());
        int initialLikedPlaylistsCount = playlistsScreen.getPlaylistItemCount();

        // unlike and assert item now gone
        playlistsScreen.get(0).clickOverflow().toggleLike();
        assertThat(playlistsScreen.getPlaylistItemCount(), is(initialLikedPlaylistsCount - 1));

        // assert item has been unliked on posted playlists tab
        playlistsScreen.touchPostedPlaylistsTab();
        assertThat(playlistsScreen.get(0).clickOverflow().isLiked(), is(false));
    }
}
