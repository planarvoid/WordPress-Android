package com.soundcloud.android.tests.player;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.BlockedTrackTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

@BlockedTrackTest
public class GeoBlockTest extends ActivityTest<MainActivity> {

    private PlaylistDetailsScreen playlistScreen;

    public GeoBlockTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.freeNonMonetizedUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        playlistScreen = mainNavHelper.goToCollections()
                                      .clickPlaylistsPreview()
                                      .scrollToAndClickPlaylistWithTitle("Geoblock Test");
    }

    public void testSkipsBlockedTracks() throws Exception {
        final String title = playlistScreen.clickFirstTrack()
                                           .waitForExpandedPlayer()
                                           .waitForTheExpandedPlayerToPlayNextTrack()
                                           .getTrackTitle();
        assertThat(title, is("Post - Geoblock"));
    }

    public void testSwipeForwardToBlockedTrackShowsGeoError() throws Exception {
        final VisualPlayerElement visualPlayerElement = playlistScreen.clickFirstTrack()
                                                                      .waitForExpandedPlayer()
                                                                      .swipeNext();

        assertThat(visualPlayerElement.isErrorBlockedVisible(), is(true));
        assertThat(visualPlayerElement.swipePrevious().isExpandedPlayerPaused(), is(true));
    }

    public void testSwipeBackToBlockedTrackShowsGeoError() throws Exception {
        final VisualPlayerElement visualPlayerElement = playlistScreen.clickFirstTrack()
                                                                      .waitForTheExpandedPlayerToPlayNextTrack()
                                                                      .swipePrevious();


        assertThat(visualPlayerElement.isErrorBlockedVisible(), is(true));
        assertThat(visualPlayerElement.swipeNext().isExpandedPlayerPaused(), is(true));
    }

    public void testSwipeForwardToBlockedTrackCanStartStation() throws Exception {
        final VisualPlayerElement visualPlayerElement = playlistScreen.clickFirstTrack()
                                                                      .waitForExpandedPlayer()
                                                                      .swipeNext();

        String originalTitle = visualPlayerElement.getTrackTitle();

        visualPlayerElement.startStationFromUnplayableTrack().swipeNext();

        assertThat(visualPlayerElement.getTrackPageContext(), containsString(originalTitle));
    }

    public void testPlayGeoBlockedTrackCanStillLike() {
        // this should eventually be clickFirstBlockedTrack() when the UI is there
        final VisualPlayerElement visualPlayerElement = playlistScreen.clickFirstTrack()
                                                                      .waitForExpandedPlayer()
                                                                      .swipeNext();

        assertThat(visualPlayerElement.likeButton().hasVisibility(), is(true));
        assertThat(visualPlayerElement.shareButton().hasVisibility(), is(true));
    }

    public void testPlayGeoBlockedTrackCannotBeToggledToPlay() {
        // this should eventually be clickFirstBlockedTrack() when the UI is there
        final VisualPlayerElement visualPlayerElement = playlistScreen.clickFirstTrack()
                                                                      .waitForExpandedPlayer()
                                                                      .swipeNext().pressBackToCollapse()
                                                                      .toggleFooterPlay()
                                                                      .tapFooter();

        assertThat(visualPlayerElement.isExpandedPlayerPlaying(), is(false));

        visualPlayerElement.clickArtwork();

        assertThat(visualPlayerElement.isExpandedPlayerPlaying(), is(false));
    }
}
