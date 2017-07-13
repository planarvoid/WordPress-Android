package com.soundcloud.android.tests.stations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.discovery.OldDiscoveryScreen;
import com.soundcloud.android.screens.elements.StationsBucketElement;
import com.soundcloud.android.screens.stations.LikedStationsScreen;
import com.soundcloud.android.screens.stations.StationHomeScreen;
import com.soundcloud.android.tests.ActivityTest;

public class LikedStationsTest extends ActivityTest<MainActivity> {

    public LikedStationsTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.stationsUser;
    }

    @Override
    protected void beforeStartActivity() {
        getFeatureFlags().disable(Flag.DISCOVER_BACKEND);
    }

    @Override
    protected void tearDown() throws Exception {
        getFeatureFlags().reset(Flag.DISCOVER_BACKEND);
        super.tearDown();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ConfigurationHelper.disableStationsOnboarding(getActivity());
    }

    public void testLikeAndUnlikeStation() {
        final OldDiscoveryScreen discoveryScreen = mainNavHelper.goToOldDiscovery();
        final StationsBucketElement stationsBucketElement = discoveryScreen.stationsRecommendationsBucket();

        final String title = stationsBucketElement.getFirstStation().getTitle();
        StationHomeScreen stationHome = stationsBucketElement.getFirstStation().open();
        assertThat(stationHome.stationTitle(), containsString(title.substring(0, Math.min(20, title.length()))));

        boolean liked = stationHome.isStationLiked();
        if (!liked) {
            stationHome.clickStationLike();
            assertThat(stationHome.isStationLiked(), is(true));
        }

        stationHome.goBack();

        final LikedStationsScreen likedStationsScreen = mainNavHelper.goToCollections().clickStations();
        stationHome = likedStationsScreen.clickStationWithTitle(title);
        assertThat(stationHome.isStationLiked(), is(true));

        stationHome.clickStationLike();
        assertThat(stationHome.isStationLiked(), is(false));

    }

}
