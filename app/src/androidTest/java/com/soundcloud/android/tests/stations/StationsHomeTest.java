package com.soundcloud.android.tests.stations;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.Issue;
import com.soundcloud.android.framework.annotation.StationsTabTest;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ViewAllStationsScreen;
import com.soundcloud.android.screens.elements.StationsBucketElement;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

@StationsTabTest
public class StationsHomeTest extends ActivityTest<LauncherActivity> {

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
    }

    public void testSavedStationsIsAvailable() {
        assertTrue(mainNavHelper.goToStationsHome().getSavedStationsBucket().isVisible());
    }

    public void testRecentStationsBucket() {
        assertTrue(mainNavHelper.goToStationsHome().getRecentStationsBucket().isVisible());
    }

    public void testTrackRecommendationsIsAvailable() {
        assertTrue(mainNavHelper.goToStationsHome().getTrackRecommendationsBucket().isVisible());
    }

    public void testCuratorRecommendationsIsAvailable() {
        assertTrue(mainNavHelper.goToStationsHome().getCuratorRecommendationsBucket().isVisible());
    }

    public void testGenreRecommendationsIsAvailable() {
        assertTrue(mainNavHelper.goToStationsHome().getGenreRecommendationsBucket().isVisible());
    }

    public void testStartedStationShouldBeAddedToRecentStations() {
        final String stationTitle = startStationAndReturnTitle();

        final StationsBucketElement recentStations = mainNavHelper
                .goToStationsHome()
                .getRecentStationsBucket();

        assertThat(recentStations.getFirstStation().getTitle(), is(equalTo(stationTitle)));
        ViewAllStationsScreen viewAllStationsScreen = recentStations.clickViewAll();
        assertThat(viewAllStationsScreen.getFirstStation().getTitle(), is(equalTo(stationTitle)));
    }

    @Issue(ref="https://github.com/soundcloud/SoundCloud-Android/issues/4578")
    public void testStartStationFromBucket() {
        final String stationTitle = startStationAndReturnTitle();

        final VisualPlayerElement player = mainNavHelper
                .goToStationsHome()
                .getRecentStationsBucket()
                .findStation(With.text(stationTitle))
                .click();

        assertThat(player, is(visible()));
    }

    @Issue(ref="https://github.com/soundcloud/SoundCloud-Android/issues/4578")
    public void testStartStationFromViewAllStations() {
        final String stationTitle = startStationAndReturnTitle();

        final VisualPlayerElement player = mainNavHelper
                .goToStationsHome()
                .getRecentStationsBucket()
                .clickViewAll()
                .findStation(With.text(stationTitle))
                .click();

        assertThat(player, is(visible()));
    }

    private String startStationAndReturnTitle() {
        final PlaylistDetailsScreen playlistDetailsScreen = mainNavHelper
                .goToCollections()
                .scrollToAndClickPlaylistWithTitle("track-stations");

        playlistDetailsScreen.waitForContentAndRetryIfLoadingFailed();

        final TrackItemElement track = playlistDetailsScreen.getTrack(1);
        final String title = track.getTitle();

        track.clickOverflowButton().clickStartStation().pressBackToCollapse();
        solo.goBack();

        return title;
    }
}
