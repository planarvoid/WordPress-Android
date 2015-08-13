package com.soundcloud.android.tests.stations;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class StartStationTest extends ActivityTest<LauncherActivity> {
    private StreamScreen streamScreen;

    public StartStationTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.streamUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setRequiredEnabledFeatures(Flag.STATIONS);

        //FIXME: This is a workaround for #1487
        waiter.waitForContentAndRetryIfLoadingFailed();
        streamScreen = new StreamScreen(solo);
    }

    public void testStartStation() {
        final VisualPlayerElement player = startStationFromFirstTrack();

        assertThat(player, is(visible()));
    }

    public void testStartStationVisibleButDisabledWhenUserHasNoNetworkConnectivity() {
        toastObserver.observe();
        networkManagerClient.switchWifiOff();

        final VisualPlayerElement playerElement = startStationFromFirstTrack();

        assertThat(playerElement, is(not(visible())));
        assertFalse(toastObserver.wasToastObserved(solo.getString(R.string.unable_to_start_radio)));

        networkManagerClient.switchWifiOn();
    }

    public void testStartStationShouldResume() {
        final VisualPlayerElement player = startStationFromFirstTrack();

        // We swipe next twice in order to ensure the database is correctly
        // persisting the last played track position
        player.swipeNext();
        player.swipeNext();

        final String expectedTitle = player.getTrackTitle();
        player.swipePrevious();
        player.pressBackToCollapse();

        // Start a new play queue
        streamScreen.clickFirstTrack();
        player.pressBackToCollapse();

        final String resumedTrackTitle = startStationFromFirstTrack().getTrackTitle();
        assertEquals(expectedTitle, resumedTrackTitle);
    }

    public void testStartedStationShouldBeAddedToRecentStations() {
        final String trackTitle = streamScreen.getTrack(0).getTitle();

        startStationFromFirstTrack().pressBackToCollapse();

        assertTrue(menuScreen.open().clickStations().findStation(trackTitle).isVisible());
    }

    private VisualPlayerElement startStationFromFirstTrack() {
        return streamScreen.clickFirstTrackOverflowButton().clickStartStation();
    }
}
