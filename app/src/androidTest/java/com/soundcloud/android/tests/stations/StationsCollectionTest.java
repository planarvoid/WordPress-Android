package com.soundcloud.android.tests.stations;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.StationsSoftLaunchTest;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.CollectionsScreen;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ViewAllStationsScreen;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

@StationsSoftLaunchTest
public class StationsCollectionTest extends ActivityTest<LauncherActivity> {
    private CollectionsScreen collectionsScreen;
    private String stationNameInPlayer;

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
        setRequiredEnabledFeatures(Flag.STATIONS_SOFT_LAUNCH);

        collectionsScreen = mainNavHelper.goToCollections();
    }

    public void testRecentStationsBucket() {
        assertTrue(collectionsScreen.clickRecentStations().isVisible());
    }

    public void testStartedStationShouldBeAddedToRecentStations() {
        final String stationTrackTitle = startStationAndReturnTitle();

        final ViewAllStationsScreen viewAllStationsScreen = collectionsScreen.clickRecentStations();

        assertThat(viewAllStationsScreen.getFirstStation().getTitle(), is(equalTo(stationTrackTitle)));
        VisualPlayerElement player = viewAllStationsScreen.getFirstStation().click();
        assertThat(player, is(visible()));
        assertThat(player.getTrackPageContext(), containsString(stationTrackTitle));
    }

    private String startStationAndReturnTitle() {
        final PlaylistDetailsScreen playlistDetailsScreen = collectionsScreen
                .clickPlaylistWithTitle("track-stations");

        playlistDetailsScreen.waitForContentAndRetryIfLoadingFailed();

        final TrackItemElement track = playlistDetailsScreen.getTrack(1);
        final String title = track.getTitle();

        final VisualPlayerElement player = track.clickOverflowButton().clickStartStation();
        assertTrue(waiter.waitForPlaybackToBePlaying());
        player.pressBackToCollapse();
        solo.goBack();

        return title;
    }
}
