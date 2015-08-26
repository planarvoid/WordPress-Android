package com.soundcloud.android.tests.stations;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ViewAllStationsScreen;
import com.soundcloud.android.screens.elements.StationsBucketElement;
import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class StartStationTest extends ActivityTest<LauncherActivity> {

    private PlaylistDetailsScreen playlistDetailsScreen;

    public StartStationTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.stationsUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setRequiredEnabledFeatures(Flag.STATIONS);

        playlistDetailsScreen = menuScreen
                .open()
                .clickPlaylists()
                .clickPlaylist(With.text("track-stations"));

        playlistDetailsScreen.waitForContentAndRetryIfLoadingFailed();
    }

    public void testStartStation() {
        final VisualPlayerElement player = playlistDetailsScreen.startStationFromFirstTrack();

        assertThat(player, is(visible()));
    }

    public void testStartStationVisibleButDisabledWhenUserHasNoNetworkConnectivity() {
        toastObserver.observe();
        networkManagerClient.switchWifiOff();

        final VisualPlayerElement playerElement = playlistDetailsScreen.startStationFromFirstTrack();

        assertThat(playerElement, is(not(visible())));
        assertFalse(toastObserver.wasToastObserved(solo.getString(R.string.unable_to_start_radio)));

        networkManagerClient.switchWifiOn();
    }

    public void testStartStationShouldResume() {
        final VisualPlayerElement player = playlistDetailsScreen.startStationFromFirstTrack();

        // We swipe next twice in order to ensure the database is correctly
        // persisting the last played track position
        player.swipeNext();
        player.swipeNext();

        final String expectedTitle = player.getTrackTitle();
        player.swipePrevious();
        player.pressBackToCollapse();

        // Start a new play queue
        playlistDetailsScreen.clickFirstTrack();
        player.pressBackToCollapse();

        final String resumedTrackTitle = playlistDetailsScreen.startStationFromFirstTrack().getTrackTitle();
        assertEquals(expectedTitle, resumedTrackTitle);
    }

    public void testStartedStationShouldBeAddedToRecentStations() {
        final String currentStationName = startFiveRadiosAndReturnTheStationName();

        solo.goBack();

        final StationsBucketElement recentStations = menuScreen.open().clickStations().getRecentStationsBucket();

        assertEquals(recentStations.getFirstStation().getTitle(), currentStationName);
        final ViewAllStationsScreen viewAllStationsScreen = recentStations.clickViewAll();
        assertEquals(viewAllStationsScreen.getFirstStation().getTitle(), currentStationName);
    }

    public void testStartStationFromBucket() throws Exception {
        final String currentStationName = startFiveRadiosAndReturnTheStationName();

        solo.goBack();
        final StationsBucketElement recentStations = menuScreen.open().clickStations().getRecentStationsBucket();

        recentStations.findStation(With.text(currentStationName)).click();
    }

    public void testStartStationFromViewAllStations() throws Exception {
        final String currentStationName = startFiveRadiosAndReturnTheStationName();

        solo.goBack();
        final ViewAllStationsScreen viewAllStationsScreen = menuScreen.open().clickStations().getRecentStationsBucket().clickViewAll();

        viewAllStationsScreen.findStation(With.text(currentStationName)).click();
    }

    private String startFiveRadiosAndReturnTheStationName() {
        playlistDetailsScreen.getTrack(0).clickOverflowButton().clickStartStation().pressBackToCollapse();
        playlistDetailsScreen.getTrack(1).clickOverflowButton().clickStartStation().pressBackToCollapse();
        playlistDetailsScreen.getTrack(2).clickOverflowButton().clickStartStation().pressBackToCollapse();
        playlistDetailsScreen.getTrack(3).clickOverflowButton().clickStartStation().pressBackToCollapse();

        final TrackItemElement track = playlistDetailsScreen.getTrack(4);
        final String title = track.getTitle();

        track.clickOverflowButton().clickStartStation().pressBackToCollapse();
        return title;
    }
}
