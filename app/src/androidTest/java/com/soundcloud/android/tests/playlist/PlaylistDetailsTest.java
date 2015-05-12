package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.TestUser.playlistUser;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.AddToPlaylistScreen;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class PlaylistDetailsTest extends ActivityTest<LauncherActivity> {
    private PlaylistsScreen playlistsScreen;
    private PlaylistDetailsScreen playlistDetailsScreen;

    public PlaylistDetailsTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        playlistUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        menuScreen = new MenuScreen(solo);
        //FIXME: This is a workaround for #1487
        waiter.waitForContentAndRetryIfLoadingFailed();

        playlistsScreen = menuScreen.open().clickPlaylist();
        waiter.waitForContentAndRetryIfLoadingFailed();
        playlistDetailsScreen = playlistsScreen.clickPlaylistAt(0);
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

    public void testRemovingAndAddingTrackFromPlaylist() throws Exception {
        String title = playlistDetailsScreen.getTitle();
        int initialTrackCount = playlistsScreen.getLoadedTrackCount();

        VisualPlayerElement player = playlistDetailsScreen.clickFirstTrack();
        assertTrue("Player did not expand", player.waitForExpandedPlayer());
        player.pressBackToCollapse();

        TrackItemMenuElement menu = playlistDetailsScreen.clickFirstTrackOverflowButton();
        menu.clickRemoveFromPlaylist();

        assertThat(playlistsScreen.getLoadedTrackCount(), is(initialTrackCount - 1));

        player.tapFooter();
        AddToPlaylistScreen addToPlaylistScreen = player.clickMenu().clickAddToPlaylist();
        addToPlaylistScreen.clickPlaylistWithTitleFromPlayer(title);
        player.pressBackToCollapse();

        assertThat(playlistsScreen.getLoadedTrackCount(), is(initialTrackCount));
    }
}
