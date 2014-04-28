package com.soundcloud.android.explore;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.PlayerScreen;
import com.soundcloud.android.screens.explore.ExploreGenreCategoryScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;

public class ExploreRecommendations extends ActivityTestCase<MainActivity> {
    private MainScreen mainScreen;
    private ExploreScreen exploreScreen;
    private ExploreGenreCategoryScreen categoryScreen;

    private PlayerScreen playerScreen;

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
        waiter.expect(playerScreen.trackTitleElement())
                .toHaveText(trackName);
    }

    public void testPlayingExploreElectronicTrack() {
        exploreScreen.touchGenresTab();
        categoryScreen = exploreScreen.clickGenreItem("Ambient");
        String trackName = categoryScreen.getTrackTitle(1);
        playerScreen = categoryScreen.playTrack(1);
        waiter.expect(playerScreen.trackTitleElement()).toHaveText(trackName);

        // make sure recommendations load
        playerScreen.swipeLeft();
        assertNotSame(trackName, playerScreen.trackTitle());
    }
}
