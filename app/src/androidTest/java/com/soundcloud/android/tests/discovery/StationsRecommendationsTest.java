package com.soundcloud.android.tests.discovery;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.elements.StationsBucketElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class StationsRecommendationsTest extends TrackingActivityTest<MainActivity> {
    private static final String RECOMMENDED_STATIONS_PLAYING_SPEC = "playing_recommended_station2";

    public StationsRecommendationsTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.offlineUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testStartSuggestedStationFromDiscovery() {

        startEventTracking();
        final DiscoveryScreen discoveryScreen = mainNavHelper.goToDiscovery();
        final StationsBucketElement stationsBucketElement = discoveryScreen.stationsRecommendationsBucket();

        final String title = stationsBucketElement.getFirstStation().getTitle();
        final VisualPlayerElement playerElement = stationsBucketElement.getFirstStation()
                                                                       .click()
                                                                       .waitForExpandedPlayerToStartPlaying()
                                                                       .clickArtwork();

        assertThat(playerElement.isExpanded(), is(true));
        assertThat(playerElement.getTrackPageContext(), is(equalTo(title)));

        playerElement.pressBackToCollapse().waitForCollapsedPlayer();
        assertThat(playerElement.isCollapsed(), is(true));
        assertThat(stationsBucketElement.getFirstStation().isPlaying(), is(true));

        finishEventTracking(RECOMMENDED_STATIONS_PLAYING_SPEC);
    }

}
