package com.soundcloud.android.tests.stations;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.EventTrackingTest;
import com.soundcloud.android.framework.annotation.StationsSoftLaunchTest;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

@EventTrackingTest
@StationsSoftLaunchTest
public class TrackingStartStation extends TrackingActivityTest<MainActivity> {
    private static final String START_STATION_FROM_PLAYLIST = "audio-events-v1-start_station_from_playlist";

    public TrackingStartStation() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.stationsUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testStartStationFromPlaylist() throws Exception {
        startEventTracking();

        final VisualPlayerElement player = mainNavHelper.goToCollections()
                .scrollToAndClickPlaylistWithTitle("track-stations")
                .clickFirstTrackOverflowButton()
                .clickStartStation();

        assertTrue(player.isExpandedPlayerPlaying());
        player.swipeNext();
        assertTrue(player.isExpandedPlayerPlaying());
        player.clickArtwork();

        finishEventTracking(START_STATION_FROM_PLAYLIST);
    }
}
