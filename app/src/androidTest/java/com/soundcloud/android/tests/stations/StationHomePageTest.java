package com.soundcloud.android.tests.stations;

import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.stations.StationHomeScreen;

/**
 * This test tests exactly the same functionality as LegacyStationTest, the only difference is that it takes
 * into account the station home page. LegacyStationTest should be replaces by LikeStationsTest after StationHome
 * (StationInfoPage) is released
 */
public class StationHomePageTest extends TrackingActivityTest<LauncherActivity> {

    private static final String START_STATION_FROM_TRACK_ITEM = "audio-events-v1-open_station_from_playlist";

    private PlaylistDetailsScreen playlistDetailsScreen;

    public StationHomePageTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.stationsUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        playlistDetailsScreen = mainNavHelper.goToCollections()
                                             .clickPlaylistsPreview()
                                             .scrollToAndClickPlaylistWithTitle("track-stations");

        playlistDetailsScreen.waitForContentAndRetryIfLoadingFailed();
    }

    public void testOpenStationFromTrackItem() {
        startEventTracking();

        final VisualPlayerElement player = playlistDetailsScreen
                .findAndClickFirstTrackOverflowButton()
                .clickStation()
                .clickPlay()
                .waitForExpandedPlayerToStartPlaying();

        assertThat(player, is(visible()));
        assertTrue(player.isExpandedPlayerPlaying());
        player.swipeNext();
        assertTrue(player.isExpandedPlayerPlaying());
        player.clickArtwork();

        finishEventTracking(START_STATION_FROM_TRACK_ITEM);
    }

    public void testOpenStationFromPlayer() {
        VisualPlayerElement player = playlistDetailsScreen.clickFirstTrack();
        final String originalTitle = player.getTrackTitle();

        final StationHomeScreen stationHomeScreen = player.clickMenu()
                                                          .clickOpenStation();
        assertTrue(stationHomeScreen.isVisible());

        player = stationHomeScreen
                .clickPlay()
                .waitForExpandedPlayerToStartPlaying()
                .swipeNext();

        assertThat(player.getTrackPageContext(), containsString(originalTitle));
    }

    public void testOpenStationShouldResume() {
        final VisualPlayerElement player = playlistDetailsScreen
                .findAndClickFirstTrackOverflowButton()
                .clickStation()
                .clickPlay()
                .waitForExpandedPlayerToStartPlaying();

        // We swipe next twice in order to ensure the database is correctly
        // persisting the last played track position
        player.swipeNext();
        player.swipeNext();

        final String expectedTitle = player.getTrackTitle();
        player.swipePrevious();
        assertThat(waiter.waitForPlaybackToBePlaying(), is(true));
        player.pressBackToCollapse();
        solo.goBack();

        // Start a new play queue
        playlistDetailsScreen.clickFirstTrack();
        assertThat(waiter.waitForPlaybackToBePlaying(), is(true));
        player.pressBackToCollapse();

        final StationHomeScreen stationHomeScreen = playlistDetailsScreen.findAndClickFirstTrackOverflowButton()
                                                                         .clickStation();
        final String resumedTrackTitle = stationHomeScreen.clickPlay().getTrackTitle();
        assertThat(expectedTitle, is(equalTo(resumedTrackTitle)));

        // If you play the same station, it should simply expand the player without changing tracks
        player.pressBackToCollapse();
        final String resumeCurrentlyPlayingStationTitle = stationHomeScreen.clickPlay().getTrackTitle();
        assertThat(resumedTrackTitle, is(equalTo(resumeCurrentlyPlayingStationTitle)));
    }

}
