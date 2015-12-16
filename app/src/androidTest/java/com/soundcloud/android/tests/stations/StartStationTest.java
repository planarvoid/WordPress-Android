package com.soundcloud.android.tests.stations;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.StationsSoftLaunchTest;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

@StationsSoftLaunchTest
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
        setRequiredEnabledFeatures(Flag.STATIONS_SOFT_LAUNCH);

        playlistDetailsScreen = mainNavHelper.goToCollections()
                .clickPlaylistWithTitle("track-stations");

        playlistDetailsScreen.waitForContentAndRetryIfLoadingFailed();
    }

    public void testStartStationFromTrackItem() {
        final VisualPlayerElement player = playlistDetailsScreen.startStationFromFirstTrack();

        assertThat(player, is(visible()));
    }

    public void testStartStationFromPlayer() {
        final VisualPlayerElement player = playlistDetailsScreen.clickFirstTrack();
        final String originalTitle = player.getTrackTitle();

        player.clickMenu().clickStartStation();
        player.swipeNext();

        assertThat(player.getTrackPageContext(), containsString(originalTitle));
    }

    public void testStartStationVisibleButDisabledWhenUserHasNoNetworkConnectivity() {
        toastObserver.observe();
        networkManagerClient.switchWifiOff();

        final VisualPlayerElement playerElement = playlistDetailsScreen.startStationFromFirstTrack();

        assertThat(playerElement, is(not(visible())));
        assertFalse(toastObserver.wasToastObserved(solo.getString(R.string.stations_unable_to_start_station)));

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
        assertThat(waiter.waitForPlaybackToBePlaying(), is(true));
        player.pressBackToCollapse();

        // Start a new play queue
        playlistDetailsScreen.clickFirstTrack();
        assertThat(waiter.waitForPlaybackToBePlaying(), is(true));
        player.pressBackToCollapse();

        final String resumedTrackTitle = playlistDetailsScreen.startStationFromFirstTrack().getTrackTitle();
        assertThat(expectedTitle, is(equalTo(resumedTrackTitle)));

        // If you play the same station, it should simply expand the player without changing tracks
        player.pressBackToCollapse();
        final String resumeCurrentlyPlayingStationTitle = playlistDetailsScreen.startStationFromFirstTrack().getTrackTitle();
        assertThat(resumedTrackTitle, is(equalTo(resumeCurrentlyPlayingStationTitle)));
    }
}
