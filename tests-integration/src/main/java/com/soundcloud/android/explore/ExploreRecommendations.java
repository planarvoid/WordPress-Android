package com.soundcloud.android.explore;

import static com.soundcloud.android.tests.TestUser.testUser;

import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.screens.PlayerScreen;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.AccountAssistant;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.Waiter;

public class ExploreRecommendations extends ActivityTestCase<Home> {
    private Waiter waiter;
    private ExploreScreen exploreScreen;
    private PlayerScreen playerScreen;

    public ExploreRecommendations() {
        super(Home.class);
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

    public void testPlayingTrack() {
        menuScreen.openExplore();
//        String trackName = exploreScreen.clickTrack(1);
//        assertEquals(trackName, playerScreen.trackTitle());
    }

    @Override
    protected void tearDown() throws Exception {
        AccountAssistant.logOut(getInstrumentation());
        super.tearDown();
    }
}
