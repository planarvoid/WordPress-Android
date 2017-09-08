package com.soundcloud.android.tests.stream;

import static com.soundcloud.android.R.string;
import static com.soundcloud.android.R.string.like_toast_overflow_action;
import static com.soundcloud.android.R.string.reposted_to_followers;
import static com.soundcloud.android.R.string.unlike_toast_overflow_action;
import static com.soundcloud.android.R.string.unposted_to_followers;
import static com.soundcloud.android.framework.TestUser.followingOneTrackOnePlaylistUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.StreamCardElement;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class PlaylistCardEngagementsTest extends ActivityTest<MainActivity> {

    private StreamScreen streamScreen;

    public PlaylistCardEngagementsTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return followingOneTrackOnePlaylistUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        streamScreen = new StreamScreen(solo);
    }

    @Test
    public void testLikeAndRepostPlaylist() throws Exception {
        StreamCardElement card = streamScreen.scrollToFirstPlaylistTrackCard();
        assertToggleLike(card);
        assertToggleRepost(card);
    }

    private void assertToggleLike(StreamCardElement card) {
        TrackItemMenuElement overflow = card.clickOverflowButton();
        boolean liked = overflow.isLiked();
        overflow.toggleLike();

        assertThat(streamScreen, is(visible()));

        final String likeToastMessage = getLikeToastMessage(liked);
        assertTrue("Did not observe a toast with a message: " + likeToastMessage,
                   waiter.expectToastWithText(toastObserver, likeToastMessage));
    }

    private void assertToggleRepost(StreamCardElement card) {
        TrackItemMenuElement overflow = card.clickOverflowButton();
        boolean reposted = overflow.isReposted();
        overflow.toggleRepost();

        assertThat(streamScreen, is(visible()));

        final String repostToastMessage = getRepostToastMessage(reposted);
        assertTrue("Did not observe a toast with a message: " + repostToastMessage,
                   waiter.expectToastWithText(toastObserver, repostToastMessage));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }

    private String getLikeToastMessage(boolean liked) {
        return solo.getString(liked ? unlike_toast_overflow_action : like_toast_overflow_action);
    }

    private String getRepostToastMessage(boolean reposted) {
        return solo.getString(reposted ? unposted_to_followers : reposted_to_followers);
    }
}
