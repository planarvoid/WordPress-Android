package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.TestUser.playlistUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.AddToPlaylistScreen;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.tests.ActivityTest;

public class ItemOverflowTest extends ActivityTest<MainActivity> {
    private TrackLikesScreen trackLikesScreen;

    public ItemOverflowTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return playlistUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        trackLikesScreen = mainNavHelper.goToTrackLikes();
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public void testClickingAddToPlaylistOverflowMenuItemOpensDialog() {
        final AddToPlaylistScreen addToPlaylistScreen = trackLikesScreen
                .clickFirstTrackOverflowButton()
                .clickAddToPlaylist();

        assertThat(addToPlaylistScreen, is(visible()));
    }

}
