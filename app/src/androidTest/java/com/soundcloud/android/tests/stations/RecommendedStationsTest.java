package com.soundcloud.android.tests.stations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.discovery.OldDiscoveryScreen;
import com.soundcloud.android.screens.elements.StationsBucketElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.stations.StationHomeScreen;
import com.soundcloud.android.tests.ActivityTest;

public class RecommendedStationsTest extends ActivityTest<MainActivity> {
    private static final String RECOMMENDED_STATIONS_HOME_SPEC = "specs/recommended_station_home_page.spec";

    public RecommendedStationsTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.offlineUser;
    }

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
