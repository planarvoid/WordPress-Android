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

    public void testShouldShowInterstitial() {
        playerElement.swipeNext(); // to monetizableTrack
        assertTrue(playerElement.waitForPlayState());
        playerElement.waitForInterstitialToLoad();
        assertThat("Should display interstitial", playerElement.isInterstitialVisible());
    }
}
