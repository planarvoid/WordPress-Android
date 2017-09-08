package com.soundcloud.android.tests.stations;

import static com.soundcloud.android.framework.TestUser.stationsUser;
import static com.soundcloud.android.framework.helpers.ConfigurationHelper.disableStationsOnboarding;
import static com.soundcloud.android.properties.Flag.DISCOVER_BACKEND;
import static java.lang.Math.min;
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
import org.junit.Test;

public class LikedStationsTest extends ActivityTest<MainActivity> {

    public LikedStationsTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return stationsUser;
    }

    @Override
    protected void beforeActivityLaunched() {
        getFeatureFlags().disable(DISCOVER_BACKEND);
    }

    @Override
    public void tearDown() throws Exception {
        getFeatureFlags().reset(DISCOVER_BACKEND);
        super.tearDown();
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        disableStationsOnboarding(activityTestRule.getActivity());
    }

    @Test
    public void testLikeAndUnlikeStation() throws Exception {
        final OldDiscoveryScreen discoveryScreen = mainNavHelper.goToOldDiscovery();
        final StationsBucketElement stationsBucketElement = discoveryScreen.stationsRecommendationsBucket();

        final String title = stationsBucketElement.getFirstStation().getTitle();
        StationHomeScreen stationHome = stationsBucketElement.getFirstStation().open();
        assertThat(stationHome.stationTitle(), containsString(title.substring(0, min(20, title.length()))));

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
