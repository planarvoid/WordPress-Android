package com.soundcloud.android.tests.stream;

import static com.soundcloud.android.framework.TestUser.streamUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.R;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.PlaylistItemOverflowMenu;
import com.soundcloud.android.tests.ActivityTest;

public class PlaylistCardEngagementsTest extends ActivityTest<MainActivity> {

    private StreamScreen streamScreen;

    public PlaylistCardEngagementsTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        streamUser.logIn(getInstrumentation().getTargetContext());
        streamScreen = new StreamScreen(solo);
    }

    public void testClickingToggleRepostPlaylistFromOverflowMenu() {
        PlaylistItemOverflowMenu playlistItemOverflowMenu =
                streamScreen.clickFirstPlaylistOverflowButton();

        boolean reposted = playlistItemOverflowMenu.isReposted();
        playlistItemOverflowMenu.toggleRepost();

        assertThat(streamScreen, is(visible()));

        final String repostToastMessage = getRepostToastMessage(reposted);
        assertTrue("Did not observe a toast with a message: " + repostToastMessage,
                waiter.expectToastWithText(toastObserver, repostToastMessage));
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

    private String getRepostToastMessage(boolean reposted) {
        return solo.getString(reposted ? R.string.unposted_to_followers : R.string.reposted_to_followers);
    }
}
