package com.soundcloud.android.tests.stations;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.StationsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class StationsHomeTest extends ActivityTest<LauncherActivity> {
    private StationsScreen stationsScreen;

    public StationsHomeTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.stationsUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setRequiredEnabledFeatures(Flag.STATIONS);

        stationsScreen = menuScreen.open().clickStations();

        // TODO: Sync Stations upon login. Then remove this line!
        stationsScreen.pullToRefresh();
    }

    public void testSavedStationsIsAvailable() {
        assertTrue(stationsScreen.getSavedStationsBucket().isVisible());
    }

    public void testRecentStationsBucket() {
        assertTrue(stationsScreen.getRecentStationsBucket().isVisible());
    }

    public void testTrackRecommendationsIsAvailable() {
        assertTrue(stationsScreen.getTrackRecommendationsBucket().isVisible());
    }

    public void testCuratorRecommendationsIsAvailable() {
        assertTrue(stationsScreen.getCuratorRecommendationsBucket().isVisible());
    }

    public void testGenreRecommendationsIsAvailable() {
        assertTrue(stationsScreen.getGenreRecommendationsBucket().isVisible());
    }
}
