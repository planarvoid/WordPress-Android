package com.soundcloud.android.tests.player.ads;

import static com.soundcloud.android.framework.helpers.PlayerHelper.assertSwipeToNextTrack;
import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.activity.resolve.ResolveBaseTest;

public abstract class AdBaseTest extends ResolveBaseTest {

    protected VisualPlayerElement playerElement;
    protected PlaylistDetailsScreen playlistDetailsScreen;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        playAdPlaylist();
        runConditions.setRunCondition(BuildConfig.RUN_ADS_TEST);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.adTestUser;
    }

    /**
     * We have 2 tracks before the monetizable track, due to a known behaviour (#2025),
     * in which you will not get an ad on a fresh install if you open play queue and the monetizable track is the second track
     * This will happen when we stop using the policies endpoint to get track policies on a play queue change
     */
    private void playAdPlaylist() {
        playlistDetailsScreen = new PlaylistDetailsScreen(solo);
        playerElement = playlistDetailsScreen.clickFirstTrack();
        assertTrue("Player did not expand", playerElement.waitForExpandedPlayer().isExpanded());
        playerElement.waitForAudioAdToBeDone(); // let 1st track play for 30ish sec
        assertSwipeToNextTrack(playerElement);
        assertTrue("Playback did not play", waiter.waitForPlaybackToBePlaying());
        playerElement.waitForAdToBeFetched(); // let 2nd track play for 5 sec
    }

    protected void swipeToAd() {
        playerElement.swipeNext();
        assertTrue("Ad Page was not visible", playerElement.waitForAdPage());
    }
}
