package com.soundcloud.android.tests.stations;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.LegacyStationTest;
import com.soundcloud.android.framework.annotation.StationsHomeTest;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.stations.RecentStationsScreen;
import com.soundcloud.android.tests.ActivityTest;

public class StationsCollectionTest extends ActivityTest<LauncherActivity> {
    private CollectionScreen collectionScreen;

    public StationsCollectionTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.stationsUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        collectionScreen = mainNavHelper.goToCollections();
    }

    @LegacyStationTest
    public void testStartedStationShouldBeAddedToRecentStations() {
        final String stationTrackTitle = startStationAndReturnTitle(false);
        final RecentStationsScreen recentStationsScreen = mainNavHelper.goToCollections().clickRecentStations();

        assertTrue(recentStationsScreen.isVisible());
        assertThat(recentStationsScreen.getFirstStation().getTitle(), is(equalTo(stationTrackTitle)));

        final VisualPlayerElement player = recentStationsScreen
                .getFirstStation()
                .click()
                .waitForExpandedPlayerToStartPlaying();

        assertThat(player, is(visible()));
        assertThat(player.getTrackPageContext(), containsString(stationTrackTitle));
    }

    @StationsHomeTest
    public void testOpenedStationShouldBeAddedToRecentStations() {
        final String stationTrackTitle = startStationAndReturnTitle(true);
        final RecentStationsScreen recentStationsScreen = mainNavHelper.goToCollections().clickRecentStations();

        assertTrue(recentStationsScreen.isVisible());
        assertThat(recentStationsScreen.getFirstStation().getTitle(), is(equalTo(stationTrackTitle)));

        final VisualPlayerElement player = recentStationsScreen
                .getFirstStation()
                .open()
                .clickPlay()
                .waitForExpandedPlayerToStartPlaying();

        assertThat(player, is(visible()));
        assertThat(player.getTrackPageContext(), containsString(stationTrackTitle));
    }

    private String startStationAndReturnTitle(boolean withHomeStation) {
        final PlaylistDetailsScreen playlistDetailsScreen = collectionScreen
                .scrollToAndClickPlaylistWithTitle("track-stations");

        playlistDetailsScreen.waitForContentAndRetryIfLoadingFailed();

        final TrackItemElement track = playlistDetailsScreen.scrollToAndGetFirstTrackItem();
        final String title = track.getTitle();

        startPlayingStation(track, withHomeStation)
                .waitForExpandedPlayerToStartPlaying()
                .pressBackToCollapse();

        solo.goBack();
        if (withHomeStation) {
            solo.goBack();
        }

        return title;
    }

    private VisualPlayerElement startPlayingStation(TrackItemElement track, boolean isStationHomeEnabled) {
        if (isStationHomeEnabled) {
            return track.clickOverflowButton().clickStation().clickPlay();
        }
        return track.clickOverflowButton().clickStartStation();
    }
}
