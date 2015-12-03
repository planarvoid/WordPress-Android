package com.soundcloud.android.tests.player;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.BlockedTrackTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
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
        final String errorReason = playlistScreen.clickFirstTrack()
                .waitForExpandedPlayer()
                .swipeNext()
                .errorReason();

        assertThat(errorReason, is("Not available yet in your country"));
    }

    public void testSwipeBackToBlockedTrackShowsGeoError() throws Exception {
        final String errorReason = playlistScreen.clickFirstTrack()
                .waitForExpandedPlayer()
                .waitForTheExpandedPlayerToPlayNextTrack()
                .swipePrevious()
                .errorReason();

        assertThat(errorReason, is("Not available yet in your country"));
    }
}
