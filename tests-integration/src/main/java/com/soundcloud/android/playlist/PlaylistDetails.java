package com.soundcloud.android.playlist;

import static com.soundcloud.android.tests.TestUser.playlistUser;
import static com.soundcloud.android.tests.hamcrest.IsVisible.Visible;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.LegacyPlayerScreen;
import com.soundcloud.android.screens.MenuScreen;
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

        waiter.waitForItemCountToIncrease(solo.getCurrentListView().getAdapter(),0);
        menuScreen.open().clickPlaylist();
        waiter.waitForContentAndRetryIfLoadingFailed();
        solo.clickInList(0);

        playlistDetailsScreen = new PlaylistDetailsScreen(solo);
    }

    public void testPlaylistDetailsScreenShouldBeVisibleOnPlaylistClick() {
        assertThat(playlistDetailsScreen, is(Visible()));
    }

    public void testHeaderPlayClickShouldNotOpenPlayer() {
        LegacyPlayerScreen playerScreen = new LegacyPlayerScreen(solo);
        playlistDetailsScreen.clickHeaderPlay();
        assertThat(playerScreen, is(not(Visible())));

        playlistDetailsScreen.clickHeaderPause();
        assertThat(playlistDetailsScreen, is(Visible()));
    }

    public void testToggleStateIsNotCheckedAfterPausingPlayer() {
        playlistDetailsScreen.clickHeaderPlay();
        playlistDetailsScreen.clickHeaderPause();

        assertThat(playlistDetailsScreen.isPlayToggleChecked(), is(false));
    }
}
