package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.TestUser.streamUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.framework.screens.AddToPlaylistsScreen;
import com.soundcloud.android.framework.screens.MenuScreen;
import com.soundcloud.android.framework.screens.ProfileScreen;
import com.soundcloud.android.tests.ActivityTest;

public class ItemOverflowTest extends ActivityTest<LauncherActivity> {

    private ProfileScreen screen;

    public ItemOverflowTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        setDependsOn(Feature.TRACK_ITEM_OVERFLOW);
        streamUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();

        menuScreen = new MenuScreen(solo);
        screen = menuScreen.open().clickUserProfile();
    }

    public void testClickingAddToPlaylistOverflowMenuItemOpensDialog() {
        screen
                .clickFirstTrackOverflowButton()
                .clickAdToPlaylist();

        final AddToPlaylistsScreen addToPlaylistsScreen = new AddToPlaylistsScreen(solo);
        assertThat(addToPlaylistsScreen, is(visible()));
    }
}
