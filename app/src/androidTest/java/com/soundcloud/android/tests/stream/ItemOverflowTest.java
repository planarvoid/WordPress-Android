package com.soundcloud.android.tests.stream;

import static com.soundcloud.android.framework.TestUser.streamUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.R;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.AddToPlaylistScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.PlaylistItemOverflowMenu;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.tests.ActivityTest;

@Deprecated // should be merged with CardEngagementTest when new stream is out
public class ItemOverflowTest extends ActivityTest<LauncherActivity> {

    private StreamScreen streamScreen;

    public ItemOverflowTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        streamScreen = new StreamScreen(solo);
    }

    @Override
    protected void logInHelper() {
        streamUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testClickingAddToPlaylistOverflowMenuItemOpensDialog() {
        final AddToPlaylistScreen addToPlaylistScreen = streamScreen
                .clickFirstTrackOverflowButton()
                .clickAddToPlaylist();

        assertThat(addToPlaylistScreen, is(visible()));
    }

    public void testClickingToggleLikeFromOverflowMenu() {
        TrackItemMenuElement trackItemMenuElement =
                streamScreen.clickFirstTrackOverflowButton();

        boolean liked = trackItemMenuElement.isLiked();
        trackItemMenuElement.toggleLike();

        assertThat(streamScreen, is(visible()));

        final String likeToastMessage = getLikeToastMessage(liked);
        assertTrue("Did not observe a toast with a message: " + likeToastMessage,
                waiter.expectToastWithText(toastObserver, likeToastMessage));
    }

    public void testClickingToggleLikePlaylistFromOverflowMenu() {
        PlaylistItemOverflowMenu playlistItemOverflowMenu =
                streamScreen.clickFirstPlaylistOverflowButton();

        boolean liked = playlistItemOverflowMenu.isLiked();
        playlistItemOverflowMenu.toggleLike();

        assertThat(streamScreen, is(visible()));

        final String likeToastMessage = getLikeToastMessage(liked);
        assertTrue("Did not observe a toast with a message: " + likeToastMessage,
                waiter.expectToastWithText(toastObserver, likeToastMessage));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }

    private String getLikeToastMessage(boolean liked) {
        return solo.getString(liked ? R.string.unlike_toast_overflow_action : R.string.like_toast_overflow_action);
    }

}
