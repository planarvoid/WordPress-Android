package com.soundcloud.android.tests.player.ads;

public class LeaveBehindTest extends AdBaseTest {

    public void testFinishAdShouldShowLeaveBehind() {
        playMonetizablePlaylist();

        swipeToAd();
        playerElement.waitForAdToBeDone();
        playerElement.waitForAdOverlayToLoad();
        assertTrue(playerElement.isLeaveBehindVisible());
    }
}
