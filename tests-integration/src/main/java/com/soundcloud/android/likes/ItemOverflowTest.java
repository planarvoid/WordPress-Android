package com.soundcloud.android.likes;

import static com.soundcloud.android.tests.TestUser.playlistUser;
import static com.soundcloud.android.tests.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.screens.AddToPlaylistsScreen;
import com.soundcloud.android.screens.LikesScreen;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.tests.ActivityTestCase;

public class ItemOverflowTest extends ActivityTestCase<MainActivity> {
    private LikesScreen screen;

    public ItemOverflowTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        setDependsOn(Feature.TRACK_ITEM_OVERFLOW);
        playlistUser.logIn(getInstrumentation().getTargetContext());

        super.setUp();

        menuScreen = new MenuScreen(solo);
        screen = menuScreen.open().clickLikes();
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void testClickingAddToPlaylistOverflowMenuItemOpensDialog() {
        screen
                .clickFirstTrackOverflowButton()
                .clickAdToPlaylist();

        final AddToPlaylistsScreen addToPlaylistsScreen = new AddToPlaylistsScreen(solo);
        assertThat(addToPlaylistsScreen, is(visible()));
    }

}
