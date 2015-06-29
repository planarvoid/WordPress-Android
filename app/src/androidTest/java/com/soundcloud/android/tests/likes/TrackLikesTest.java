package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.TrackLikesScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class TrackLikesTest extends ActivityTest<MainActivity> {

    private TrackLikesScreen likesScreen;

    public TrackLikesTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        likesScreen = menuScreen.open().clickLikes();
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    @Override
    protected void logInHelper() {
        TestUser.likesUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testClickingShuffleButtonOpensPlayer() {
        VisualPlayerElement playerElement = likesScreen.clickShuffleButton();

        assertThat(playerElement, is(visible()));
    }

    public void testClickingTrackOpensPlayer() {
        VisualPlayerElement playerElement = likesScreen.clickTrack(1);

        assertThat(playerElement, is(visible()));
    }

    public void testLoadsNextPage() {
        int numberOfTracks = likesScreen.getLoadedTrackCount();
        assertThat(numberOfTracks, is(greaterThan(0)));

        likesScreen.scrollToBottomOfTracksListAndLoadMoreItems();

        assertThat(likesScreen.getLoadedTrackCount(), is(greaterThan(numberOfTracks)));
    }

    public void testLikeChangeOnPlayerUpdatesTrackLikesScreen() {
        final int initialLikedTracksCount = likesScreen.getTotalLikesCount();

        final VisualPlayerElement player = likesScreen.clickTrack(0);

        player.tapToggleLikeButton();
        player.pressCloseButton();

        assertThat(likesScreen.getTotalLikesCount(), equalTo(initialLikedTracksCount - 1));

        player.tapFooter();
        player.tapToggleLikeButton();
        player.pressBackToCollapse();

        assertThat(likesScreen.getTotalLikesCount(), equalTo(initialLikedTracksCount));
    }
}
