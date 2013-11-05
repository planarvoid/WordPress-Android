package com.soundcloud.android.explore;

import static com.soundcloud.android.tests.TestUser.testUser;

import com.soundcloud.android.activity.MainActivity;
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
        AccountAssistant.loginAs(getInstrumentation(), testUser.getUsername(), testUser.getPassword());
        super.setUp();

        exploreScreen = new ExploreScreen(this);
        playerScreen        = new PlayerScreen(solo);
        waiter              = new Waiter(solo);
        waiter.waitForListContent();
    }

    public void testPlayingTrendingMusicTrack() {
        menuScreen.openExplore();
        String trackName = exploreScreen.playPopularTrack(10);
        assertEquals(trackName, playerScreen.trackTitle());
    }

    public void testPlayingExploreElectronicTrack() {
        menuScreen.openExplore();
        exploreScreen.touchGenresTab();
        exploreScreen.clickElectronicGenre();
        String trackName = exploreScreen.playPopularTrack(8);
        assertEquals(trackName, playerScreen.trackTitle());
    }

    @Override
    protected void tearDown() throws Exception {
        AccountAssistant.logOut(getInstrumentation());
        super.tearDown();
    }
}
