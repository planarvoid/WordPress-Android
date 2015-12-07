package com.soundcloud.android.tests.player.ads;

import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.framework.annotation.AdsTest;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

@AdsTest
public class LeaveBehindTest extends AdBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.AUDIO_AD_AND_LEAVE_BEHIND_PLAYLIST_URI;
    }

    public void testFinishAdShouldShowLeaveBehind() {
        swipeToAd();
        playerElement.waitForAdToBeDone();
        playerElement.waitForLeaveBehindToLoad();
        assertThat("Should display leave behind", playerElement.isLeaveBehindVisible());
    }
}
