package com.soundcloud.android.tests.stations;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.framework.annotation.StationsTest;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.StationsScreen;
import com.soundcloud.android.screens.ViewAllStationsScreen;
import com.soundcloud.android.screens.elements.StationsBucketElement;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

@StationsTest
@Ignore
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
        setRequiredEnabledFeatures(Flag.STATIONS_SOFT_LAUNCH, Flag.STATIONS_HOME);

        stationsScreen = menuScreen.open().clickStations();
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

    public void testStartedStationShouldBeAddedToRecentStations() {
        final String stationTitle = startStationAndReturnTitle();

        final StationsBucketElement recentStations = menuScreen.open().clickStations().getRecentStationsBucket();

        assertThat(recentStations.getFirstStation().getTitle(), is(equalTo(stationTitle)));
        ViewAllStationsScreen viewAllStationsScreen = recentStations.clickViewAll();
        assertThat(viewAllStationsScreen.getFirstStation().getTitle(), is(equalTo(stationTitle)));
    }

    public void testStartStationFromBucket() throws Exception {
        final String stationTitle = startStationAndReturnTitle();

        final VisualPlayerElement player = menuScreen
                .open()
                .clickStations()
                .getRecentStationsBucket()
                .findStation(With.text(stationTitle))
                .click();

        assertThat(player, is(visible()));
    }

    public void testStartStationFromViewAllStations() throws Exception {
        final String stationTitle = startStationAndReturnTitle();

        final VisualPlayerElement player = menuScreen
                .open()
                .clickStations()
                .getRecentStationsBucket()
                .clickViewAll()
                .findStation(With.text(stationTitle))
                .click();

        assertThat(player, is(visible()));
    }

    private String startStationAndReturnTitle() {
        final PlaylistDetailsScreen playlistDetailsScreen = menuScreen
                .open()
                .clickPlaylists()
                .clickPlaylist(With.text("track-stations"));

        playlistDetailsScreen.waitForContentAndRetryIfLoadingFailed();

        final TrackItemElement track = playlistDetailsScreen.getTrack(1);
        final String title = track.getTitle();

        track.clickOverflowButton().clickStartStation().pressBackToCollapse();
        solo.goBack();

        return title;
    }
}
