package com.soundcloud.android.tests.profile;

import static com.soundcloud.android.framework.TestUser.streamUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.AddToPlaylistScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class ItemOverflowTest extends ActivityTest<LauncherActivity> {

    private ProfileScreen screen;

    public ItemOverflowTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return streamUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        screen = mainNavHelper.goToMyProfile();
    }

    @Test
    public void testClickingAddToPlaylistOverflowMenuItemOpensDialog() throws Exception {
        // TODO: fix selector here.
        // It currently fails to select the overflow menu even though it's there
        screen.clickFirstTrackOverflowButton()
              .clickAddToPlaylist();

        final AddToPlaylistScreen addToPlaylistScreen = new AddToPlaylistScreen(solo);
        assertThat(addToPlaylistScreen, is(visible()));
    }
}
