package com.soundcloud.android.tests.stream;

import static com.soundcloud.android.framework.TestUser.streamUser;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.AddToPlaylistScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.StreamCardElement;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;

public class CardEngagementTest extends TrackingActivityTest<MainActivity> {

    private static final String REPOST_ENGAGEMENTS_FROM_STREAM = "stream_engagements_repost_scenario";
    private static final String LIKE_ENGAGEMENTS_FROM_STREAM = "stream_engagements_like_scenario";

    private StreamScreen streamScreen;

    public CardEngagementTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        streamScreen = new StreamScreen(solo);
    }

    @Override
    protected TestUser getUserForLogin() {
        return streamUser;
    }

    public void testStreamItemActions() {
        startEventTracking();

        // Scroll to item
        StreamCardElement track = streamScreen.scrollToFirstNotPromotedTrackCard();

        assertLikeTrack(track);
        assertRepostTrack(track);
        assertClickOnAvatarAndGoBack(track);
        assertClickOnNameAndGoBack(track);
        assertAddToPlaylist(track);
    }

    private void assertAddToPlaylist(StreamCardElement track) {
        final AddToPlaylistScreen addToPlaylistScreen = track.clickOverflowButton()
                .clickAddToPlaylist();

        assertThat(addToPlaylistScreen, is(visible()));
    }

    private void assertClickOnNameAndGoBack(StreamCardElement track) {
        ProfileScreen profileScreen = track.clickArtistName();
        assertThat(profileScreen, is(visible()));
        profileScreen.goBack();
    }

    private void assertClickOnAvatarAndGoBack(StreamCardElement track) {
        ProfileScreen profileScreen = track.clickUserAvatar();
        assertThat(profileScreen, is(visible()));
        profileScreen.goBack();
    }

    private void assertRepostTrack(StreamCardElement track) {
        startEventTracking();
        boolean reposted = track.isReposted();

        track.toggleRepost();

        // Not cool -- wait for the UI thread to catch up with the test runner.
        waiter.waitTwoSeconds();

        assertThat(streamScreen, is(visible()));
        assertThat(track.isReposted(), is(not(reposted)));

        final String repostToastMessage = getRepostToastMessage(reposted);
        assertTrue("Did not observe a toast with a message: " + repostToastMessage,
                   waiter.expectToastWithText(toastObserver, repostToastMessage));

        // toggle from overflow
        TrackItemMenuElement menuElement = track.clickOverflowButton();
        menuElement.toggleRepost();
        finishEventTracking(REPOST_ENGAGEMENTS_FROM_STREAM);
    }

    private void assertLikeTrack(StreamCardElement track) {
        startEventTracking();
        boolean liked = track.isLiked();

        track.clickOverflowButton().toggleLike();

        // Not cool -- wait for the UI thread to catch up with the test runner.
        waiter.waitTwoSeconds();

        assertThat(track.isLiked(), is(!liked));

        final String likeToastMessage = getLikeToastMessage(liked);
        assertTrue("Did not observe a toast with a message: " + likeToastMessage,
                   waiter.expectToastWithText(toastObserver, likeToastMessage));

        track.toggleLike();
        assertThat(track.isLiked(), is(liked));
        finishEventTracking(LIKE_ENGAGEMENTS_FROM_STREAM);
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
