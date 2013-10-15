package com.soundcloud.android.explore;

import static com.soundcloud.android.tests.TestUser.testUser;

import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.screens.PlayerScreen;
import com.soundcloud.android.screens.explore.DiscoveryScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;
import com.soundcloud.android.tests.Waiter;

public class ExploreRecommendations extends ActivityTestCase<Home> {
    private Waiter waiter;
    private DiscoveryScreen discoveryScreen;
    private PlayerScreen playerScreen;

    public ExploreRecommendations() {
        super(Home.class);
    }

    @Override
    public void setUp() throws Exception {
        IntegrationTestHelper.loginAs(getInstrumentation(), testUser.getUsername(), testUser.getPassword());
        super.setUp();

        discoveryScreen = new DiscoveryScreen(solo, waiter, this);
        playerScreen        = new PlayerScreen(solo);
        waiter              = new Waiter(solo);

        waiter.waitForListContent();
    }

    public void testPlayingTrack() {
        menuScreen.openExplore();
        String trackName = discoveryScreen.clickTrack(1);
        assertEquals(trackName, playerScreen.trackTitle());
    }

    @Override
    protected void tearDown() throws Exception {
        IntegrationTestHelper.logOut(getInstrumentation());
        super.tearDown();
    }
}
