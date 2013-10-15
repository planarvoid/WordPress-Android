package com.soundcloud.android.explore;

import static com.soundcloud.android.tests.TestUser.testUser;

import com.soundcloud.android.activity.landing.Home;
import com.soundcloud.android.screens.explore.DiscoveryScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;
import com.soundcloud.android.tests.Waiter;

import android.content.Context;
import android.net.wifi.WifiManager;

public class PopularMusic extends ActivityTestCase<Home> {
    private Waiter waiter;
    private DiscoveryScreen discoveryScreen;

    public PopularMusic() {
        super(Home.class);
    }

    @Override
    public void setUp() throws Exception {
        IntegrationTestHelper.loginAs(getInstrumentation(), testUser.getUsername(), testUser.getPassword());
        super.setUp();

        waiter = new Waiter(solo);
        discoveryScreen = new DiscoveryScreen(solo, waiter, this);
    }

    public void testPopularMusicDisplaysTracks() {
        menuScreen.openExplore();
        assertEquals("Popular music", discoveryScreen.getActiveTabName());
        assertEquals(15, discoveryScreen.getItemsOnList());

        discoveryScreen.scrollDown();
        assertEquals(30, discoveryScreen.getItemsOnList());
    }

    public void testPopularMusicRefresh() {
        menuScreen.openExplore();
        discoveryScreen.scrollDown();
        discoveryScreen.pullToRefresh();
        assertEquals(15, discoveryScreen.getItemsOnList());
    }

    public void testNoNetworkConnectivity() {
        menuScreen.openExplore();
        turnWifi(false);
        discoveryScreen.scrollDown();
        discoveryScreen.pullToRefresh();
        assertEquals(15, discoveryScreen.getItemsOnList());

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
