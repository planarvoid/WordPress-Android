package com.soundcloud.android.explore;

import static com.soundcloud.android.tests.TestUser.testUser;

import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.screens.PlayerScreen;
import com.soundcloud.android.screens.explore.DiscoveryScreen;
import com.soundcloud.android.screens.explore.DiscoveryCategoryTracksScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;
import com.soundcloud.android.tests.Waiter;

/**
 * Created with IntelliJ IDEA.
 * User: slawomirsmiechura
 * Date: 9/19/13
 * Time: 1:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class ExploreRecommendations extends ActivityTestCase<Home> {
    private Waiter waiter;
    private DiscoveryScreen discoveryScreen;
    private DiscoveryCategoryTracksScreen discoveryCategoryTracksScreen;
    private PlayerScreen playerScreen;

    public ExploreRecommendations() {
        super(Home.class);
    }

    @Override
    public void setUp() throws Exception {
        IntegrationTestHelper.loginAs(getInstrumentation(), testUser.getUsername(), testUser.getPassword());
        super.setUp();

        discoveryScreen = new DiscoveryScreen(solo);
        discoveryCategoryTracksScreen = new DiscoveryCategoryTracksScreen(solo);
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
