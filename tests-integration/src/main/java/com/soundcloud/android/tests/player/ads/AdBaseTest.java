package com.soundcloud.android.tests.player.ads;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.tests.R;

public class AdBaseTest extends ActivityTest<MainActivity> {

    protected VisualPlayerElement playerElement;
    protected PlaylistDetailsScreen playlistDetailsScreen;

    public AdBaseTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.adUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
        setRunBasedOnResource(R.bool.run_ads_tests);
    }

    /**
     *
     * We have 2 tracks before the monetizable track, due to a known behaviour (#2025),
     * in which you will not get an ad on a fresh install if you open play queue and the monetizable track is the second track
     * This will happen when we stop using the policies endpoint to get track policies on a play queue change
     *
     */
    protected void playAdPlaylistWithText(String text) {
        playlistDetailsScreen = menuScreen.open().clickPlaylist().clickPlaylist(With.text(text));
        playerElement = playlistDetailsScreen.clickFirstTrack();
        assertTrue("Player did not expanded", playerElement.waitForExpandedPlayer());
        playerElement.swipeNext();
        assertTrue("Playback did not play", waiter.waitForPlaybackToBePlaying());
        playerElement.waitForAdToBeFetched();
    }

    protected void playMonetizablePlaylist() {
        playAdPlaylistWithText("[auto] AudioAd and LeaveBehind Playlist");
    }

    protected void playInterstitialPlaylist() {
        playAdPlaylistWithText("[auto] Interstitial Playlist");
    }

    protected void swipeToAd() {
        playerElement.swipeNext();
        assertTrue("Ad Page was not visible", playerElement.waitForAdPage());
    }
}
