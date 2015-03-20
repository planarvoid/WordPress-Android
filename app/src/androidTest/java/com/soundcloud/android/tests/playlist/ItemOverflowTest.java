package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.TestUser.playlistUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.AddToPlaylistScreen;
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
                .clickAddToPlaylist();

        final AddToPlaylistScreen addToPlaylistScreen = new AddToPlaylistScreen(solo);
        assertThat(addToPlaylistScreen, is(visible()));
    }

    //Ignored because of unchangeable configuration for debug build type.
    //Github issue: https://github.com/soundcloud/SoundCloud-Android/issues/2914
    public void ignore_testClickingOnNewPlaylistItemOpensDialog() {
        playlistDetailsScreen
                .scrollToFirstTrackItem()
                .clickFirstTrackOverflowButton()
                .clickAddToPlaylist();

        final AddToPlaylistScreen addToPlaylistScreen = new AddToPlaylistScreen(solo);
        final CreatePlaylistScreen createPlaylistScreen = addToPlaylistScreen.clickCreateNewPlaylist();
        assertThat(createPlaylistScreen, is(visible()));
        assertThat(createPlaylistScreen.offlineCheck().isVisible(), is(false));
    }
}
