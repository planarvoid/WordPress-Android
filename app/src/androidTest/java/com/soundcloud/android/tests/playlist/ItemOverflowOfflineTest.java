package com.soundcloud.android.tests.playlist;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.enableOfflineContent;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.OfflineContentHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.AddToPlaylistScreen;
import com.soundcloud.android.screens.CreatePlaylistScreen;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.tests.ActivityTest;

import android.content.Context;

public class ItemOverflowOfflineTest extends ActivityTest<MainActivity> {

    private PlaylistDetailsScreen playlistDetailsScreen;

    public ItemOverflowOfflineTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        final Context context = getInstrumentation().getTargetContext();

        resetOfflineSyncState(context);
        TestUser.offlinePlaylistUser.logIn(context);
        super.setUp();

        menuScreen = new MenuScreen(solo);
        //FIXME: This is a workaround for #1487
        waiter.waitForContentAndRetryIfLoadingFailed();

        PlaylistsScreen playlistsScreen = menuScreen.open().clickPlaylist();
        waiter.waitForContentAndRetryIfLoadingFailed();
        playlistDetailsScreen = playlistsScreen.clickPlaylistAt(0);
    }

    public void testClickingOnNewPlaylistItemOpensDialogWithOfflineOption() {
        enableOfflineContent(getActivity());

        playlistDetailsScreen
                .scrollToFirstTrackItem()
                .clickFirstTrackOverflowButton()
                .clickAdToPlaylist();

        final AddToPlaylistScreen addToPlaylistScreen = new AddToPlaylistScreen(solo);
        final CreatePlaylistScreen createPlaylistScreen = addToPlaylistScreen.clickCreateNewPlaylist();
        assertThat(createPlaylistScreen, is(visible()));
        assertThat(createPlaylistScreen.offlineCheck().isVisible(), is(true));
    }

    private void resetOfflineSyncState(Context context) {
        ConfigurationHelper.disableOfflineSync(context);
        OfflineContentHelper.clearOfflineContent(context);
    }
}
