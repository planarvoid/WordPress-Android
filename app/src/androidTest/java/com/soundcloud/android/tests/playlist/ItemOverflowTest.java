package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.TestUser.playlistUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.AddToPlaylistsScreen;
import com.soundcloud.android.screens.CreatePlaylistScreen;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class ItemOverflowTest extends ActivityTest<LauncherActivity> {

    private PlaylistDetailsScreen playlistDetailsScreen;

    public ItemOverflowTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        playlistUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();

        menuScreen = new MenuScreen(solo);
        //FIXME: This is a workaround for #1487
        waiter.waitForContentAndRetryIfLoadingFailed();

        PlaylistsScreen playlistsScreen = menuScreen.open().clickPlaylist();
        waiter.waitForContentAndRetryIfLoadingFailed();
        playlistDetailsScreen = playlistsScreen.clickPlaylistAt(0);
    }

    public void testClickingAddToPlaylistOverflowMenuItemOpensDialog() {
        playlistDetailsScreen
                .scrollToFirstTrackItem()
                .clickFirstTrackOverflowButton()
                .clickAdToPlaylist();

        final AddToPlaylistsScreen addToPlaylistsScreen = new AddToPlaylistsScreen(solo);
        assertThat(addToPlaylistsScreen, is(visible()));
    }

    public void testClickingOnNewPlaylistItemOpensDialog() {
        playlistDetailsScreen
                .scrollToFirstTrackItem()
                .clickFirstTrackOverflowButton()
                .clickAdToPlaylist();

        final AddToPlaylistsScreen addToPlaylistsScreen = new AddToPlaylistsScreen(solo);
        final CreatePlaylistScreen createPlaylistScreen = addToPlaylistsScreen.clickCreateNewPlaylist();
        assertThat(createPlaylistScreen, is(visible()));
        assertThat(createPlaylistScreen.offlineCheck().isVisible(), is(false));
    }
}
