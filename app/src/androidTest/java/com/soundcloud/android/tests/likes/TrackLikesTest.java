package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import org.hamcrest.core.Is;

public class TrackLikesTest extends TrackingActivityTest<MainActivity> {
    private static final String TEST_SCENARIO_LIKES = "audio-events-v1-main-likes";
    private static final String TEST_LIKES_SHUFFLE = "likes-shuffle-events";

    private TrackLikesScreen likesScreen;

    public TrackLikesTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.likesUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testLikesScreen() throws Exception {
        likesScreen = mainNavHelper.goToTrackLikes();
        waiter.waitForContentAndRetryIfLoadingFailed();

        assertShuffleStartsPlaying();
        assertPlaysAndPausesTrack();
        assertLikeChangeUpdatesLikesScreen();
        assertLoadsNextPageOfLikes();
    }

    private void assertPlaysAndPausesTrack() {
        final VisualPlayerElement playerElement = likesScreen.clickFirstLongTrack();

        assertThat(playerElement, Is.is(visible()));
        assertThat(playerElement, Is.is(playing()));

        startEventTracking();

        playerElement.clickArtwork();

        assertThat(playerElement, Is.is(not(playing())));

        finishEventTracking(TEST_SCENARIO_LIKES);
        playerElement.pressBackToCollapse();
    }

    private void assertLoadsNextPageOfLikes() {
        int numberOfTracks = likesScreen.getLoadedTrackCount();
        assertThat(numberOfTracks, is(greaterThan(0)));

        likesScreen.scrollToBottomOfTracksListAndLoadMoreItems();

        assertThat(likesScreen.getLoadedTrackCount(), is(greaterThan(numberOfTracks)));
    }

    private void assertLikeChangeUpdatesLikesScreen() {
        final int initialLikedTracksCount = likesScreen.getTotalLikesCount();
        final VisualPlayerElement player = likesScreen.clickFirstLongTrack();

        player.tapToggleLikeButton();
        player.pressCloseButton();

        assertThat(likesScreen.getTotalLikesCount(), equalTo(initialLikedTracksCount - 1));

        player.tapFooter();
        player.tapToggleLikeButton();
        player.pressBackToCollapse();

        assertThat(likesScreen.getTotalLikesCount(), equalTo(initialLikedTracksCount));
    }

    private void assertShuffleStartsPlaying() {
        startEventTracking();

        VisualPlayerElement playerElement = likesScreen.clickShuffleButton();
        assertThat(playerElement, is(playing()));

        finishEventTracking(TEST_LIKES_SHUFFLE);
        playerElement.clickArtwork();
        playerElement.pressBackToCollapse();
    }
}
