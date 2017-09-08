package com.soundcloud.android.tests.player.ads;

import static com.soundcloud.android.tests.TestConsts.INTERSTITIAL_PLAYLIST_URI;
import static com.soundcloud.android.utils.Log.ADS_TAG;
import static com.soundcloud.android.utils.Log.d;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.framework.annotation.AdsTest;
import com.soundcloud.android.tests.TestConsts;
import com.soundcloud.android.utils.Log;
import org.junit.Test;

import android.net.Uri;

@AdsTest
public class InterstitialTest extends AdBaseTest {

    @Override
    protected Uri getUri() {
        return INTERSTITIAL_PLAYLIST_URI;
    }

    @Override
    public void setUp() throws Exception {
        d(ADS_TAG, "InterstitialTest.setUp->");
        super.setUp();
        d(ADS_TAG, "<-InterstitialTest.setUp");
    }

    @Test
    public void testShouldShowInterstitial() throws Exception {
        d(ADS_TAG, "InterstitialTest.testShouldShowInterstitial");

        d(ADS_TAG, "InterstitialTest.setUp::swipeNext->");
        playerElement.swipeNext(); // to monetizableTrack
        d(ADS_TAG, "InterstitialTest.setUp::<-swipeNext");

        d(ADS_TAG, "InterstitialTest.setUp::waitForPlayState->");
        assertTrue(playerElement.waitForPlayState());
        d(ADS_TAG, "InterstitialTest.setUp::<-waitForPlayState");

        assertThat("Display interstitial", playerElement.waitForInterstitialToBeDisplayed());
    }
}
