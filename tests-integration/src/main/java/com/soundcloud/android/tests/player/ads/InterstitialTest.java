package com.soundcloud.android.tests.player.ads;

public class InterstitialTest extends AdBaseTest {

    public void testShouldShowInterstitial() {
        playInterstitialPlaylist();

        playerElement.swipeNext(); // to monetizableTrack
        assertTrue(playerElement.waitForPlayState());
        playerElement.waitForAdOverlayToLoad();
        assertTrue(playerElement.isInterstitialVisible());
    }
}
