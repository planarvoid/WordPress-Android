package com.soundcloud.android.tests.player.ads;

import static com.soundcloud.android.tests.TestConsts.AUDIO_AD_AND_LEAVE_BEHIND_PLAYLIST_URI;
import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.framework.annotation.AdsTest;
import com.soundcloud.android.tests.TestConsts;
import org.junit.Test;

import android.net.Uri;

@AdsTest
public class LeaveBehindTest extends AdBaseTest {

    @Override
    protected Uri getUri() {
        return AUDIO_AD_AND_LEAVE_BEHIND_PLAYLIST_URI;
    }

    @org.junit.Ignore
    @Test
    public void testFinishAdShouldShowLeaveBehind() throws Exception {
        swipeToAd();
        playerElement.waitForAudioAdToBeDone();
        playerElement.waitForLeaveBehindToLoad();
        assertThat("Should display leave behind", playerElement.isLeaveBehindVisible());
    }
}
