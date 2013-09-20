package com.soundcloud.android.explore;

import static com.soundcloud.android.tests.TestUser.testUser;

import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;
import com.soundcloud.android.tests.Waiter;

import android.content.Context;
import android.net.wifi.WifiManager;

public class PopularMusic extends ActivityTestCase<Home> {
    private Waiter waiter;
    private ExploreScreen exploreScreen;

    public PopularMusic() {
        super(Home.class);
    }

    @Override
    public void setUp() throws Exception {
        IntegrationTestHelper.loginAs(getInstrumentation(), testUser.getUsername(), testUser.getPassword());
        super.setUp();

        waiter = new Waiter(solo);

        exploreScreen = new ExploreScreen(solo);
    }

    public void testPopularMusicDisplaysTracks() {
        menuScreen.openExplore();
        assertEquals("Popular music", exploreScreen.getActiveTabName());
        assertEquals(15, exploreScreen.getItemsOnList());

        exploreScreen.scrollDown();
        assertEquals(30, exploreScreen.getItemsOnList());
    }

    public void testPopularMusicRefresh() {
        menuScreen.openExplore();
        exploreScreen.scrollDown();
        exploreScreen.pullToRefresh();
        assertEquals(15, exploreScreen.getItemsOnList());
    }

    public void testNoNetworkConnectivity() {
        menuScreen.openExplore();
        turnWifi(false);
        exploreScreen.scrollDown();
        exploreScreen.pullToRefresh();
        assertEquals(15, exploreScreen.getItemsOnList());

    }

    protected void turnWifi(boolean enabled) {
//        try {
            WifiManager wifiManager = (WifiManager) getInstrumentation()
                    .getTargetContext().getSystemService(Context.WIFI_SERVICE);
            wifiManager.setWifiEnabled(enabled);
//        } catch (Exception ignored) {
            // don't interrupt test execution, if there
            // is no permission for that action
//        }
    }

    @Override
    protected void tearDown() throws Exception {
        IntegrationTestHelper.logOut(getInstrumentation());
        super.tearDown();
    }

}
