package com.soundcloud.android.tests.player.ads;

import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static com.soundcloud.android.tests.TestConsts.AUDIO_AD_WITH_TRACK_DEEPLINK_PLAYLIST_URI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;

import com.soundcloud.android.framework.annotation.AdsTest;
import com.soundcloud.android.tests.TestConsts;
import org.junit.Test;

import android.net.Uri;

@AdsTest
public class AdTrackDeeplinkTest extends AdBaseTest {

    @Override
    protected Uri getUri() {
        return AUDIO_AD_WITH_TRACK_DEEPLINK_PLAYLIST_URI;
    }

    @Test
    public void testCustomCTAButton() throws Exception {
        swipeToAd();

        playerElement.waitForPlayState();
        playerElement.clickAdArtwork();
        assertThat(playerElement, is(not(playing())));

        playerElement.clickAdCTAButton();
        playerElement.waitForPlayState();

        assertThat(playerElement.getTrackTitle(), is(equalTo("PONPONPON")));
    }
}
