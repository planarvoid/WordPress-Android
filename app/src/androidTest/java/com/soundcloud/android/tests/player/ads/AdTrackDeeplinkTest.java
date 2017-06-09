package com.soundcloud.android.tests.player.ads;

import static com.soundcloud.android.framework.matcher.player.IsPlaying.playing;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;

import com.soundcloud.android.framework.annotation.AdsTest;
import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

@AdsTest
public class AdTrackDeeplinkTest extends AdBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.AUDIO_AD_WITH_TRACK_DEEPLINK_PLAYLIST_URI;
    }

    @Ignore // FIXME https://soundcloud.atlassian.net/browse/DROID-1347
    public void testCustomCTAButton() {
        swipeToAd();

        playerElement.waitForPlayState();
        playerElement.clickAdArtwork();
        assertThat(playerElement, is(not(playing())));

        playerElement.clickAdCTAButton();
        playerElement.waitForPlayState();

        assertThat(playerElement.getTrackTitle(), is(equalTo("PONPONPON")));
    }
}
