package com.soundcloud.android.tests.stream;

import static com.soundcloud.android.framework.TestUser.streamUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.R;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.PlaylistItemOverflowMenu;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.tests.ActivityTest;

public class CardEngagementTest extends ActivityTest<MainActivity> {

    private StreamScreen streamScreen;

    public CardEngagementTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        setRequiredEnabledFeatures(Flag.NEW_STREAM);
        super.setUp();
        streamScreen = new StreamScreen(solo);
    }

    @Override
    protected void logInHelper() {
        streamUser.logIn(getInstrumentation().getTargetContext());
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

    public void testClickingToggleRepostFromOverflowMenu() {
        TrackItemMenuElement trackItemMenuElement =
                streamScreen.clickFirstTrackCardOverflowButton();

        boolean reposted = trackItemMenuElement.isReposted();
        trackItemMenuElement.toggleRepost();

        assertThat(streamScreen, is(visible()));

        final String repostToastMessage = getRepostToastMessage(reposted);
        assertTrue("Did not observe a toast with a message: " + repostToastMessage,
                waiter.expectToastWithText(toastObserver, repostToastMessage));
    }

    public void testClickingToggleRepostFromCard() {
        boolean reposted = streamScreen.firstTrackCard().isReposted();

        streamScreen.firstTrackCard().toggleRepost();

        assertThat(streamScreen, is(visible()));
        assertThat(streamScreen.firstTrackCard().isReposted(), is(not(reposted)));

        final String repostToastMessage = getRepostToastMessage(reposted);
        assertTrue("Did not observe a toast with a message: " + repostToastMessage,
                waiter.expectToastWithText(toastObserver, repostToastMessage));
    }

    public void testClickingToggleLikeFromCard() {
        boolean liked = streamScreen.firstTrackCard().isLiked();

        streamScreen.firstTrackCard().toggleLike();

        assertThat(streamScreen, is(visible()));
        assertThat(streamScreen.firstTrackCard().isLiked(), is(not(liked)));

        final String likeToastMessage = getLikeToastMessage(liked);
        assertTrue("Did not observe a toast with a message: " + likeToastMessage,
                waiter.expectToastWithText(toastObserver, likeToastMessage));
    }

    public void testClickingUserAvatarGoesToUserProfile() {
        ProfileScreen profileScreen = streamScreen.firstNotPromotedTrackCard().clickUserAvatar();

        assertThat(profileScreen, is(visible()));
    }

    public void testClickingArtistNameGoToArtistProfile() {
        ProfileScreen profileScreen = streamScreen.firstNotPromotedTrackCard().clickArtistName();

        assertThat(profileScreen, is(visible()));
    }

    private String getRepostToastMessage(boolean reposted) {
        return solo.getString(reposted ? R.string.unposted_to_followers : R.string.reposted_to_followers);
    }

    private String getLikeToastMessage(boolean liked) {
        return solo.getString(liked ? R.string.unlike_toast_overflow_action : R.string.like_toast_overflow_action);
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }


}
