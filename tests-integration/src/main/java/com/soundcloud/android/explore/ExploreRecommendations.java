package com.soundcloud.android.explore;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlayerScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.Waiter;

public class ExploreRecommendations extends ActivityTestCase<MainActivity> {
    private Waiter waiter;
    private ExploreScreen exploreScreen;
    private PlayerScreen playerScreen;

    public ExploreRecommendations() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        AccountAssistant.loginAsDefault(getInstrumentation());
        super.setUp();

        exploreScreen = new ExploreScreen(solo);
        playerScreen        = new PlayerScreen(solo);
        waiter              = new Waiter(solo);
        waiter.waitForListContent();
    }

    public void testPlayingTrendingMusicTrack() {
        menuScreen.openExplore();
        exploreScreen.touchTrendingMusicTab();
        String trackName = exploreScreen.playPopularTrack(1);
        assertEquals(trackName, playerScreen.trackTitle());
    }

    public void testPlayingExploreElectronicTrack() {
        menuScreen.openExplore();
        exploreScreen.touchGenresTab();
        exploreScreen.clickElectronicGenre();
        String trackName = exploreScreen.playPopularTrack(1);
        assertEquals(trackName, playerScreen.trackTitle());
    }
}
