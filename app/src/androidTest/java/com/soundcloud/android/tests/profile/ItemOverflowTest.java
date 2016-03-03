package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.TestUser.streamUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.AddToPlaylistScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.ActivityTest;

public class ItemOverflowTest extends ActivityTest<LauncherActivity> {

    private ProfileScreen screen;

    public ItemOverflowTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        streamUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        screen = mainNavHelper.goToMyProfile();
    }

    public void testClickingAddToPlaylistOverflowMenuItemOpensDialog() {
        screen.clickFirstTrackOverflowButton()
              .clickAddToPlaylist();

        final AddToPlaylistScreen addToPlaylistScreen = new AddToPlaylistScreen(solo);
        assertThat(addToPlaylistScreen, is(visible()));
    }
}
