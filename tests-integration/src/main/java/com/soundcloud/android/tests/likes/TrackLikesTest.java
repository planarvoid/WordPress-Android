package com.soundcloud.android.tests.likes;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.NavigationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.LikesScreen;
import com.soundcloud.android.screens.MenuScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class TrackLikesTest extends ActivityTest<MainActivity> {

    public TrackLikesTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.likesUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
    }

    public void testClickingShuffleButtonOpensPlayer() {
        LikesScreen likesScreen = NavigationHelper.openLikedTracks(new MenuScreen(solo), waiter);
        VisualPlayerElement playerElement = likesScreen.clickShuffleButton();

        assertThat(playerElement, is(visible()));
    }

    public void testClickingTrackOpensPlayer() {
        LikesScreen likesScreen = NavigationHelper.openLikedTracks(new MenuScreen(solo), waiter);
        VisualPlayerElement playerElement = likesScreen.clickItem(1);

        assertThat(playerElement, is(visible()));
    }

    public void testLoadsNextPage() {
        LikesScreen likesScreen = NavigationHelper.openLikedTracks(new MenuScreen(solo), waiter);
        int numberOfTracks = likesScreen.getLoadedTrackCount();
        assertThat(numberOfTracks, is(greaterThan(0)));

        likesScreen.scrollToBottomOfTracksListAndLoadMoreItems();

        assertThat(likesScreen.getLoadedTrackCount(), is(greaterThan(numberOfTracks)));
    }

}
