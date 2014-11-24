package com.soundcloud.android.tests.player;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.framework.screens.PlaylistDetailsScreen;
import com.soundcloud.android.framework.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.tests.R;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.with.With;

public class LeaveBehindTest extends ActivityTest<MainActivity> {

    private VisualPlayerElement playerElement;

    public LeaveBehindTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.adUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
        setRunBasedOnResource(R.bool.run_ads_tests);
    }

    public void testFinishAdShouldShowLeaveBehind() {
        playMonetizablePlaylist();

        swipeToAd();
        playerElement.waitForAdToBeDone();
        playerElement.waitForAdOverlayToLoad();
        assertTrue(playerElement.isLeaveBehindVisible());
    }

    /**
     *
     * We have 2 tracks before the monetizable track, due to a known behaviour (#2025),
     * in which you will not get an ad on a fresh install if you open play queue and the monetizable track is the second track
     * This will happen when we stop using the policies endpoint to get track policies on a play queue change
     *
     */
    private void playMonetizablePlaylist() {
        PlaylistDetailsScreen playlistDetailsScreen = menuScreen.open().clickPlaylist().clickPlaylist(With.text("[auto] AudioAd and LeaveBehind Playlist"));
        playerElement = playlistDetailsScreen.clickFirstTrack();
        playerElement.waitForExpandedPlayer();
        playerElement.swipeNext();
        playerElement.waitForAdToBeFetched();
    }

    private void swipeToAd() {
        playerElement.swipeNext();
        playerElement.waitForAdPage();
    }
}
