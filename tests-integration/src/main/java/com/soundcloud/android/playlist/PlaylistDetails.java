package com.soundcloud.android.playlist;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.PlayerScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.search.PlaylistTagsScreen;
import com.soundcloud.android.screens.search.SearchResultsScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;

public class PlaylistDetails extends ActivityTestCase<MainActivity> {

    private PlaylistDetailsScreen playlistDetailsScreen;

    public PlaylistDetails() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.defaultUser.logIn(getInstrumentation().getTargetContext());

        super.setUp();

        MainScreen mainScreen = new MainScreen(solo);
        PlaylistTagsScreen playlistTagsScreen = mainScreen.actionBar().clickSearchButton();
        SearchResultsScreen resultsScreen = playlistTagsScreen.actionBar().doSearch("dub");
        resultsScreen.touchPlaylistsTab();
        playlistDetailsScreen = resultsScreen.clickFirstPlaylistItem();
    }

    public void testPlaylistDetailsScreenShouldBeVisibleOnPlaylistClick() {
        assertEquals("Playlist details screen should be visible", true, playlistDetailsScreen.isVisible());
    }

    public void testHeaderPlayClickShouldNotOpenPlayer() {
        PlayerScreen playerScreen = playlistDetailsScreen.clickHeaderPlay();
        assertEquals("Player screen should not be visible", false, playerScreen.isVisible());

        playlistDetailsScreen.clickHeaderPause();
        assertEquals("Playlist details screen should be visible", true, playlistDetailsScreen.isVisible());
    }

    public void testToggleStateIsNotCheckedAfterPausingPlayer() {
        PlayerScreen playerScreen = playlistDetailsScreen.clickHeaderPlay();
        playerScreen.stopPlayback();

        assertEquals("Play toggle button should not be checked", false, playlistDetailsScreen.isPlayToggleChecked());
    }
}
