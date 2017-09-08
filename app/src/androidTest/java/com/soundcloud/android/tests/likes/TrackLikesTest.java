package com.soundcloud.android.tests.likes;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.soundcloud.android.framework.TestUser.likesUser;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import org.hamcrest.core.Is;
import org.junit.Test;

public class TrackLikesTest extends ActivityTest<MainActivity> {
    private static final String TEST_SCENARIO_LIKES = "specs/audio-events-v1-main-likes.spec";
    private static final String TEST_LIKES_SHUFFLE = "specs/likes-shuffle-events.spec";

    private TrackLikesScreen likesScreen;

    public TrackLikesTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return likesUser;
    }

    @Override
    protected void addInitialStubMappings() {
        // Fix : test user data changes with toggling likes... we want the data to remain the same, so just prevent the likes changes from syncing :)
        stubFor(post(urlPathEqualTo("/likes/tracks")).willReturn(aResponse().withStatus(500)));
    }

    @Test
    public void testLikesScreen() throws Exception {
        likesScreen = mainNavHelper.goToTrackLikes();
        waiter.waitForContentAndRetryIfLoadingFailed();

        assertShuffleStartsPlaying();
        assertPlaysAndPausesTrack();
        assertLikeChangeUpdatesLikesScreen();
        assertLoadsNextPageOfLikes();
    }

    private void assertPlaysAndPausesTrack() throws Exception {
        final VisualPlayerElement playerElement = likesScreen.clickFirstLongTrack();

        assertThat(playerElement, Is.is(visible()));
        assertThat(playerElement, Is.is(playing()));

        mrLocalLocal.startEventTracking();

        playerElement.clickArtwork();

        assertThat(playerElement, Is.is(not(playing())));

        mrLocalLocal.verify(TEST_SCENARIO_LIKES);
        playerElement.pressBackToCollapse();
    }

    private void assertLoadsNextPageOfLikes() {
        int numberOfTracks = likesScreen.getLoadedTrackCount();
        assertThat(numberOfTracks, is(greaterThan(0)));

        likesScreen.scrollToBottomOfTracksListAndLoadMoreItems();

        assertThat(likesScreen.getLoadedTrackCount(), is(greaterThan(numberOfTracks)));
    }

    private void assertLikeChangeUpdatesLikesScreen() {
        likesScreen.scrollToTop();

        final int initialLikedTracksCount = likesScreen.getTotalLikesCount();
        final VisualPlayerElement player = likesScreen.clickFirstLongTrack();

        player.tapToggleLikeButton();
        player.pressCloseButton();

        likesScreen.scrollToTop();
        assertThat(likesScreen.getTotalLikesCount(), equalTo(initialLikedTracksCount - 1));

        player.tapFooter();
        player.tapToggleLikeButton();
        player.pressBackToCollapse();

        assertThat(likesScreen.getTotalLikesCount(), equalTo(initialLikedTracksCount));
    }

    private void assertShuffleStartsPlaying() throws Exception {
        mrLocalLocal.startEventTracking();

        VisualPlayerElement playerElement = likesScreen.clickShuffleButton();
        assertThat(playerElement, is(playing()));

        mrLocalLocal.verify(TEST_LIKES_SHUFFLE);
        playerElement.clickArtwork();
        playerElement.pressBackToCollapse();
    }
}
