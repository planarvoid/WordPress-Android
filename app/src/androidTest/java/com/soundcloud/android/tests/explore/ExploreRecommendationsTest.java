package com.soundcloud.android.tests.explore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.explore.ExploreGenreCategoryScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTest;

public class ExploreRecommendationsTest extends ActivityTest<MainActivity> {
    private ExploreScreen exploreScreen;
    private ExploreGenreCategoryScreen categoryScreen;

    private VisualPlayerElement playerScreen;

    public ExploreRecommendationsTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.testUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        exploreScreen = new StreamScreen(solo).openExploreFromMenu();
    }

    public void testPlayingTrendingMusicTrack() {
        exploreScreen.touchTrendingMusicTab();
        String trackName = exploreScreen.getTrackTitle(1);
        playerScreen = exploreScreen.playPopularTrack(1);
        assertThat(playerScreen.getTrackTitle(), is(equalTo(trackName)));
    }

    public void testPlayingExploreElectronicTrack() {
        exploreScreen.touchGenresTab();
        categoryScreen = exploreScreen.clickGenreItem("Ambient");
        String trackName = categoryScreen.getTrackTitle(1);
        playerScreen = categoryScreen.playTrack(1);
        assertThat(playerScreen.getTrackTitle(), is(equalTo(trackName)));

        // make sure recommendations load
        playerScreen.swipeNext();
        // TODO: this produces false positives when the item is not found
        assertNotSame(trackName, playerScreen.getTrackTitle());
    }
}
