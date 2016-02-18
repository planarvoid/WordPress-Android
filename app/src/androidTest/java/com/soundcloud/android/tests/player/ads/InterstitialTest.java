package com.soundcloud.android.tests.player.ads;

import static com.soundcloud.android.utils.Log.ADS_TAG;
import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.ads.AdsOperations;
import com.soundcloud.android.framework.annotation.AdsTest;
import com.soundcloud.android.tests.TestConsts;
import com.soundcloud.android.utils.Log;

import android.net.Uri;

@AdsTest
public class InterstitialTest extends AdBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.INTERSTITIAL_PLAYLIST_URI;
    }

    @Override
    public void setUp() throws Exception {
        Log.d(AdsOperations., "InterstitialTest.setUp->");
        super.setUp();
        Log.d(ADS_TAG, "<-InterstitialTest.setUp");
    }

    public void testShouldShowInterstitial() {
        Log.d(ADS_TAG, "InterstitialTest.testShouldShowInterstitial");

        Log.d(ADS_TAG, "InterstitialTest.setUp::swipeNext->");
        playerElement.swipeNext(); // to monetizableTrack
        Log.d(ADS_TAG, "InterstitialTest.setUp::<-swipeNext");

        Log.d(ADS_TAG, "InterstitialTest.setUp::waitForPlayState->");
        assertTrue(playerElement.waitForPlayState());
        Log.d(ADS_TAG, "InterstitialTest.setUp::<-waitForPlayState");

        assertThat("Interstitial loaded", playerElement.waitForInterstitialToLoad());
        assertThat("Display interstitial", playerElement.isInterstitialVisible());
    }
}
