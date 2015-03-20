package com.soundcloud.android.tests.stream;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.AddToPlaylistScreen;
import com.soundcloud.android.screens.CreatePlaylistScreen;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.PlaylistsScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.ActivityTest;

import static com.soundcloud.android.framework.helpers.ConfigurationHelper.disableOfflineSync;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TrackItemOverflowMenuTest extends ActivityTest<LauncherActivity> {
    private StreamScreen streamScreen;

    private PlaylistDetailsScreen playlistDetailsScreen;
    private AddToPlaylistScreen addToPlaylistScreen;

    public TrackItemOverflowMenuTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        TestUser.streamUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();

        menuScreen = new MenuScreen(solo);
        //FIXME: This is a workaround for #1487
        waiter.waitForContentAndRetryIfLoadingFailed();
        streamScreen = new StreamScreen(solo);

    }

    //FIXME: https://github.com/soundcloud/SoundCloud-Android/issues/2914
    public void ignore_testClickingAddToPlaylistOverflowMenuItemOpensDialog() {

        final CreatePlaylistScreen createPlaylistScreen = streamScreen.clickFirstTrackOverflowButton().
                clickAddToPlaylist().
                clickCreateNewPlaylist();

        assertThat(createPlaylistScreen, is(visible()));
        assertThat(createPlaylistScreen.offlineCheck().isVisible(), is(false));
    }


}
