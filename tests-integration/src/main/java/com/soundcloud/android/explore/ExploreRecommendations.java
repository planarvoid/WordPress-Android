package com.soundcloud.android.explore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.explore.ExploreGenreCategoryScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;

public class ExploreRecommendations extends ActivityTestCase<MainActivity> {
    private MainScreen mainScreen;
    private ExploreScreen exploreScreen;
    private ExploreGenreCategoryScreen categoryScreen;

    private VisualPlayerElement playerScreen;

    public ExploreRecommendations() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.testUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();

        mainScreen = new MainScreen(solo);
        exploreScreen = mainScreen.openExploreFromMenu();
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
        assertNotSame(trackName, playerScreen.getTrackTitle());
    }
}
