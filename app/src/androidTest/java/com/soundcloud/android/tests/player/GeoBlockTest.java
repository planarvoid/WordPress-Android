package com.soundcloud.android.tests.player;

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
    protected void logInHelper() {
        TestUser.freeNonMonetizedUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        playlistScreen = mainNavHelper.goToCollections().clickPlaylistWithTitle("Geoblock Test");
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

        assertThat(visualPlayerElement.errorReason(), is("Not available yet in your country"));
        assertThat(visualPlayerElement.swipePrevious().isExpandedPlayerPaused(), is(true));
    }

    public void testSwipeBackToBlockedTrackShowsGeoError() throws Exception {
        final VisualPlayerElement visualPlayerElement = playlistScreen.clickFirstTrack()
                .waitForTheExpandedPlayerToPlayNextTrack()
                .swipePrevious();


        assertThat(visualPlayerElement.errorReason(), is("Not available yet in your country"));
        assertThat(visualPlayerElement.swipeNext().isExpandedPlayerPaused(), is(true));
    }

    public void testPlayGeoBlockedTrackShowsError() {
        // this should eventually be clickFirstBlockedTrack() when the UI is there
        final String errorReason = playlistScreen.scrollToPosition(1)
                .clickTrack(1)
                .waitForExpandedPlayer()
                .errorReason();

        assertThat(errorReason, is("Not available yet in your country"));
    }
}
