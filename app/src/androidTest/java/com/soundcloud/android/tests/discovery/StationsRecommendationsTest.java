package com.soundcloud.android.tests.discovery;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.EventTrackingTest;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.elements.StationsBucketElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

@EventTrackingTest
public class StationsRecommendationsTest extends TrackingActivityTest<MainActivity> {
    private static final String RECOMMENDED_STATIONS_PLAYING_SPEC = "playing_recommended_station";

    public StationsRecommendationsTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.offlineUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testStartSuggestedStationFromDiscovery() {

        final DiscoveryScreen discoveryScreen = mainNavHelper.goToDiscovery();
        final StationsBucketElement stationsBucketElement = discoveryScreen.stationsRecommendationsBucket();
        assertThat(stationsBucketElement, is(visible()));

        final String title = stationsBucketElement.getFirstStation().getTitle();
        final VisualPlayerElement playerElement = stationsBucketElement.getFirstStation()
                                                                       .click()
                                                                       .waitForExpandedPlayer()
                                                                       .clickArtwork();

        assertThat(playerElement.isExpanded(), is(true));
        assertThat(playerElement.getTrackPageContext(), is(equalTo(title)));

        playerElement.pressBackToCollapse().waitForCollapsedPlayer();
        assertThat(playerElement.isCollapsed(), is(true));
        assertThat(stationsBucketElement.getFirstStation().isPlaying(), is(true));
    }

    public void testStartSuggestedStationTracking() {
        final VisualPlayerElement player = mainNavHelper
                .goToDiscovery()
                .stationsRecommendationsBucket()
                .getFirstStation()
                .click()
                .waitForExpandedPlayerToStartPlaying();

        startEventTracking();
        // pause
        player.clickArtwork();

        finishEventTracking(RECOMMENDED_STATIONS_PLAYING_SPEC);
    }

}
