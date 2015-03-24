package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.TestUser.playlistUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.AddToPlaylistScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.tests.ActivityTest;

public class ItemOverflowTest extends ActivityTest<MainActivity> {
    private TrackLikesScreen screen;

    public ItemOverflowTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        playlistUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        menuScreen = new MenuScreen(solo);
        screen = menuScreen.open().clickLikes();
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    // temporarily ignore this test while likes syncing is flaky
    public void ignoreLikes_testClickingAddToPlaylistOverflowMenuItemOpensDialog() {
        screen
                .clickFirstTrackOverflowButton()
                .clickAddToPlaylist();

        final AddToPlaylistScreen addToPlaylistScreen = new AddToPlaylistScreen(solo);
        assertThat(addToPlaylistScreen, is(visible()));
    }

}
