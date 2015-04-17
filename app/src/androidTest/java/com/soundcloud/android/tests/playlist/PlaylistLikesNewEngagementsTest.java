package com.soundcloud.android.tests.playlist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.PlaylistsHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class PlaylistLikesNewEngagementsTest extends ActivityTest<MainActivity> {
    private PlaylistsScreen playlistsScreen;
    private PlaylistsHelper playlistsHelper;

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
        playlistsHelper = new PlaylistsHelper(this, playlistsScreen);
    }

    // Given I liked a playlist
    // Given I go the liked playlists tab on the playlists screen
    // Then the playlists should the first one
    public void testLastLikedPlaylistShouldAppearOnTop() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        final String expectedTitle = playlistsHelper.getPlaylistItemTitle(0);
        playlistsHelper.likePlaylistItemEvenIfCurrentlyLiked(0);
        assertThat(playlistsHelper.isPlaylistItemLiked(0), is(true));

        playlistsScreen.touchLikedPlaylistsTab();

        assertEquals(expectedTitle, playlistsHelper.getPlaylistItemTitle(0));
        playlistsHelper.togglePlaylistItemLike(0);
    }

    public void testLikingAndUnlikingPlaylistFromOverflowMenu() {
        // assert liked
        final String expectedTitle = playlistsHelper.getPlaylistItemTitle(0);
        playlistsHelper.likePlaylistItemEvenIfCurrentlyLiked(0);
        assertThat(playlistsHelper.isPlaylistItemLiked(0), is(true));
        playlistsScreen.touchLikedPlaylistsTab();
        assertEquals(expectedTitle, playlistsHelper.getPlaylistItemTitle(0));
        int initialLikedPlaylistsCount = playlistsScreen.getPlaylistItemCount();

        // unlike and assert item now gone
        playlistsHelper.togglePlaylistItemLike(0);
        int newLikedPlaylistsCount = playlistsScreen.getPlaylistItemCount();
        assertThat(newLikedPlaylistsCount, is(initialLikedPlaylistsCount - 1));

        // assert item has been unliked on posted playlists tab
        playlistsScreen.touchPostedPlaylistsTab();
        assertThat(playlistsHelper.isPlaylistItemLiked(0), is(false));
    }
}
