package com.soundcloud.android.tests.stations;

import static com.soundcloud.android.framework.TestUser.stationsUser;
import static com.soundcloud.android.framework.matcher.element.IsVisible.visible;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.stations.StationHomeScreen;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class StationHomePageTest extends ActivityTest<LauncherActivity> {

    private static final String START_STATION_FROM_TRACK_ITEM = "specs/audio-events-v1-open_station_from_playlist.spec";

    private PlaylistDetailsScreen playlistDetailsScreen;

    public StationHomePageTest() {
        super(LauncherActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return stationsUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        playlistDetailsScreen = mainNavHelper.goToCollections()
                                             .clickPlaylistsPreview()
                                             .scrollToAndClickPlaylistWithTitle("track-stations");

        playlistDetailsScreen.waitForContentAndRetryIfLoadingFailed();
    }

    @Test
    public void testOpenStationFromTrackItem() throws Exception {
        mrLocalLocal.startEventTracking();

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

        mrLocalLocal.verify(START_STATION_FROM_TRACK_ITEM);
    }

    @Test
    public void testOpenStationFromPlayer() throws Exception {
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
}
