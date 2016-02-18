package com.soundcloud.android.tests.player.ads;

import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.framework.annotation.AdsTest;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

@AdsTest
public class InterstitialTest extends AdBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.INTERSTITIAL_PLAYLIST_URI;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        System.out.println("InterstitialTest.setUp");
    }

    public void testShouldShowInterstitial() {
        System.out.println("InterstitialTest.testShouldShowInterstitial");

        System.out.println("InterstitialTest.setUp::swipeNext->");
        playerElement.swipeNext(); // to monetizableTrack
        System.out.println("InterstitialTest.setUp::<-swipeNext");

        System.out.println("InterstitialTest.setUp::waitForPlayState->");
        assertTrue(playerElement.waitForPlayState());
        System.out.println("InterstitialTest.setUp::<-waitForPlayState");

        assertThat("Interstitial loaded", playerElement.waitForInterstitialToLoad());
        assertThat("Display interstitial", playerElement.isInterstitialVisible());
    }
}
