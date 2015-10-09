package com.soundcloud.android.tests.stations;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.EventTrackingTest;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.StationsScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

@EventTrackingTest
@Ignore
public class TrackingStartStation extends TrackingActivityTest<MainActivity> {

    private static final String START_STATION_FROM_STATIONS_HOME = "audio-events-v1-start_station_from_stations_home";
    private static final String START_STATION_FROM_VIEW_ALL = "audio-events-v1-start_station_from_stations_view_all";
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
        setRequiredEnabledFeatures(Flag.STATIONS_SOFT_LAUNCH, Flag.STATIONS_HOME, Flag.EVENTLOGGER_AUDIO_V1);
        super.setUp();
    }

    public void testStartStationFromStationsHome() throws Exception {
        startEventTracking();

        final StationsScreen stationsScreen = menuScreen
                .open()
                .clickStations();

        stationsScreen.pullToRefresh();
        final VisualPlayerElement player = stationsScreen
                .getRecentStationsBucket()
                .getFirstStation()
                .click();

        assertTrue(player.isExpandedPlayerPlaying());

        player.clickArtwork();

        finishEventTracking(START_STATION_FROM_STATIONS_HOME);
    }

    public void testStartStationFromViewAll() throws Exception {
        startEventTracking();

        final StationsScreen stationsScreen = menuScreen
                .open()
                .clickStations();

        stationsScreen.pullToRefresh();
        final VisualPlayerElement player = stationsScreen
                .getRecentStationsBucket()
                .clickViewAll()
                .getFirstStation()
                .click();

        assertTrue(player.isExpandedPlayerPlaying());
        player.clickArtwork();

        finishEventTracking(START_STATION_FROM_VIEW_ALL);
    }

    public void testStartStationFromPlaylist() throws Exception {
        startEventTracking();

        final VisualPlayerElement player = menuScreen
                .open()
                .clickPlaylists()
                .clickPlaylist(With.text("track-stations"))
                .clickFirstTrackOverflowButton()
                .clickStartStation();

        assertTrue(player.isExpandedPlayerPlaying());
        player.clickArtwork();

        finishEventTracking(START_STATION_FROM_PLAYLIST);
    }
}
