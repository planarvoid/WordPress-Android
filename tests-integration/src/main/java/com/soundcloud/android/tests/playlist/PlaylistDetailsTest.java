package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.TestUser.playlistUser;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class PlaylistDetailsTest extends ActivityTest<LauncherActivity> {

    private PlaylistDetailsScreen playlistDetailsScreen;
    private PlaylistScreen playlistScreen;

    public PlaylistDetailsTest() {
        super(LauncherActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        playlistUser.logIn(getInstrumentation().getTargetContext());

        super.setUp();

        menuScreen = new MenuScreen(solo);
        //FIXME: This is a workaround for #1487
        waiter.waitForContentAndRetryIfLoadingFailed();

        playlistScreen = menuScreen.open().clickPlaylist();
        waiter.waitForContentAndRetryIfLoadingFailed();
        playlistScreen.clickPlaylistAt(0);
        playlistDetailsScreen = new PlaylistDetailsScreen(solo);
    }

    public void testPlaylistDetailsScreenShouldBeVisibleOnPlaylistClick() {
        assertThat(playlistDetailsScreen, is(com.soundcloud.android.framework.matcher.screen.IsVisible.visible()));
    }

    public void testHeaderPlayClickShouldNotOpenPlayer() {
        VisualPlayerElement player = new VisualPlayerElement(solo);
        assertThat(player, is(not(visible())));
        playlistDetailsScreen.clickHeaderPlay();
        assertThat(player, is(visible()));

        playlistDetailsScreen.clickHeaderPause();
        assertThat(playlistDetailsScreen, is(com.soundcloud.android.framework.matcher.screen.IsVisible.visible()));
    }

    public void testToggleStateIsNotCheckedAfterPausingPlayer() {
        playlistDetailsScreen.clickHeaderPlay();
        playlistDetailsScreen.clickHeaderPause();

        assertThat(playlistDetailsScreen.isPlayToggleChecked(), is(false));
    }
}
