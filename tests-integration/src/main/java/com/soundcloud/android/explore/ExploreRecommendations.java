package com.soundcloud.android.explore;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.PlayerScreen;
import com.soundcloud.android.screens.explore.ExploreGenreCategoryScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;

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
        AccountAssistant.loginAsDefault(getInstrumentation());
        super.setUp();

        mainScreen = new MainScreen(solo);
        exploreScreen = mainScreen.openExploreFromMenu();
    }

    public void testPlayingTrendingMusicTrack() {
        exploreScreen.touchTrendingMusicTab();
        String trackName = exploreScreen.getTrackTitle(1);
        playerScreen = exploreScreen.playPopularTrack(1);
        assertEquals(trackName, playerScreen.trackTitle());
    }

    public void testPlayingExploreElectronicTrack() {
        categoryScreen = exploreScreen.clickGenreItem("Electronic");
        String trackName = categoryScreen.getTrackTitle(1);
        playerScreen = categoryScreen.playTrack(1);
        assertEquals(trackName, playerScreen.trackTitle());
    }
}
