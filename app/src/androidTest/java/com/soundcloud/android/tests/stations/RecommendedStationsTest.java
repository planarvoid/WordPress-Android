package com.soundcloud.android.tests.stations;

import static com.soundcloud.android.framework.TestUser.offlineUser;
import static com.soundcloud.android.properties.Flag.DISCOVER_BACKEND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.discovery.OldDiscoveryScreen;
import com.soundcloud.android.screens.elements.StationsBucketElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.stations.StationHomeScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class RecommendedStationsTest extends ActivityTest<MainActivity> {
    private static final String RECOMMENDED_STATIONS_HOME_SPEC = "specs/recommended_station_home_page.spec";

    public RecommendedStationsTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return offlineUser;
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

    @Test
    public void testOpenSuggestedStationFromDiscovery() throws Exception {
        mrLocalLocal.startEventTracking();
        final OldDiscoveryScreen discoveryScreen = mainNavHelper.goToOldDiscovery();
        final StationsBucketElement stationsBucketElement = discoveryScreen.stationsRecommendationsBucket();

        final String title = stationsBucketElement.getFirstStation().getTitle();
        final StationHomeScreen stationHome = stationsBucketElement.getFirstStation()
                                                                   .open();

        final VisualPlayerElement playerElement = stationHome.clickPlay()
                                                             .waitForExpandedPlayerToStartPlaying()
                                                             .clickArtwork();

        assertThat(playerElement.isExpanded(), is(true));
        assertThat(playerElement.getTrackPageContext(), is(equalTo(title)));

        playerElement.pressBackToCollapse().waitForCollapsedPlayer();
        assertThat(playerElement.isCollapsed(), is(true));

        getSolo().goBack();
        assertThat(stationsBucketElement.getFirstStation().isPlaying(), is(true));

        mrLocalLocal.verify(RECOMMENDED_STATIONS_HOME_SPEC);
    }

}
