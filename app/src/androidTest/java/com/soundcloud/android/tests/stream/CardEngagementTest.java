package com.soundcloud.android.tests.stream;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.soundcloud.android.R.string;
import static com.soundcloud.android.R.string.like_toast_overflow_action;
import static com.soundcloud.android.R.string.reposted_to_followers;
import static com.soundcloud.android.R.string.unlike_toast_overflow_action;
import static com.soundcloud.android.R.string.unposted_to_followers;
import static com.soundcloud.android.framework.TestUser.engagementsUser;
import static com.soundcloud.android.framework.helpers.AssetHelper.readBodyOfFile;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.AddToPlaylistScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.StreamCardElement;
import com.soundcloud.android.screens.elements.TrackItemMenuElement;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

import android.content.res.Resources;

public class CardEngagementTest extends ActivityTest<MainActivity> {

    private static final String REPOST_ENGAGEMENTS_FROM_STREAM = "specs/stream_engagements_repost_scenario_v2.spec";
    private static final String LIKE_ENGAGEMENTS_FROM_STREAM = "specs/stream_engagements_like_scenario_v2.spec";

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
    protected void addInitialStubMappings() {
        Resources resources = getInstrumentation().getContext().getResources();
        String body = readBodyOfFile(resources, "engagements-user-stream.json");
        stubFor(get(urlPathMatching("/stream"))
                        .willReturn(aResponse().withStatus(200).withBody(body)));
    }

    @Override
    protected TestUser getUserForLogin() {
        return engagementsUser;
    }

    @Test
    public void testStreamItemActions() throws Exception {
        mrLocalLocal.startEventTracking();

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

    private void assertRepostTrack(StreamCardElement track) throws Exception {
        mrLocalLocal.startEventTracking();
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
        mrLocalLocal.verify(REPOST_ENGAGEMENTS_FROM_STREAM);
    }

    private void assertLikeTrack(StreamCardElement track) throws Exception {
        mrLocalLocal.startEventTracking();
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
        mrLocalLocal.verify(LIKE_ENGAGEMENTS_FROM_STREAM);
    }

    private String getRepostToastMessage(boolean reposted) {
        return solo.getString(reposted ? unposted_to_followers : reposted_to_followers);
    }

    private String getLikeToastMessage(boolean liked) {
        return solo.getString(liked ? unlike_toast_overflow_action : like_toast_overflow_action);
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }

}
