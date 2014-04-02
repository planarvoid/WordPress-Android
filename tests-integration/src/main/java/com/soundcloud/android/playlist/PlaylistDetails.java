package com.soundcloud.android.playlist;

import static com.soundcloud.android.tests.TestUser.playlistUser;

import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.PlayerScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.tests.ActivityTestCase;

public class PlaylistDetails extends ActivityTestCase<LauncherActivity> {

    private PlaylistDetailsScreen playlistDetailsScreen;

    public PlaylistDetails() {
        super(LauncherActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        playlistUser.logIn(getInstrumentation().getTargetContext());

        super.setUp();

        menuScreen = new MenuScreen(solo);
        //FIXME: This is a workaround for #1487
        waiter.waitForContentAndRetryIfLoadingFailed();
        menuScreen.open().clickPlaylist();

        solo.clickOnText("exp");

        playlistDetailsScreen = new PlaylistDetailsScreen(solo);
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
        playlistDetailsScreen.clickHeaderPlay();
        playlistDetailsScreen.clickHeaderPause();

        assertEquals("Play toggle button should not be checked", false, playlistDetailsScreen.isPlayToggleChecked());
    }
}
