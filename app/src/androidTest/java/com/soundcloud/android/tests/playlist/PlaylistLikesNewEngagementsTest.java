package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.helpers.PlaylistItemElementHelper.assertLikeActionOnPlaylist;
import static com.soundcloud.android.framework.helpers.PlaylistItemElementHelper.assertUnlikeActionOnLikedPlaylist;
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

    // **** Disabling until DROID-953 is fixed ***
    // Given I liked a playlist
    // Given I go the liked playlists tab on the playlists screen
    // Then the playlists should the first one

    /**
     * Re-enabling, as I fixed 2 bugs, and want to see if its still flaky - JS
     */
    public void testLastLikedPlaylistShouldAppearOnTop() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        final String expectedTitle = playlistsScreen.get(0).getTitle();
        assertLikeActionOnPlaylist(this, playlistsScreen.get(0));

        playlistsScreen.touchLikedPlaylistsTab();

        assertEquals(expectedTitle, playlistsScreen.get(0).getTitle());
        playlistsScreen.get(0).clickOverflow().toggleLike();
    }

    public void testLikingAndUnlikingPlaylistFromOverflowMenu() {
        // assert liked
        final String expectedTitle = playlistsScreen.get(0).getTitle();
        assertLikeActionOnPlaylist(this, playlistsScreen.get(0));

        playlistsScreen.touchLikedPlaylistsTab();
        assertEquals(expectedTitle, playlistsScreen.get(0).getTitle());
        int initialLikedPlaylistsCount = playlistsScreen.getPlaylistItemCount();

        // unlike and assert item now gone
        assertUnlikeActionOnLikedPlaylist(this, playlistsScreen.get(0));
        assertThat(playlistsScreen.getPlaylistItemCount(), is(initialLikedPlaylistsCount - 1));

        // assert item has been unliked on posted playlists tab
        playlistsScreen.touchPostedPlaylistsTab();
        assertThat(playlistsScreen.get(0).clickOverflow().isLiked(), is(false));
    }
}
