package com.soundcloud.android.activity.resolve.facebook;

import com.soundcloud.android.R;
import com.soundcloud.android.TestConsts;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.LegacyPlayerScreen;

import android.net.Uri;

public class ResolveFacebookTrackWebLinkTest extends FacebookResolveBaseTest {

    private static final String TRACK_NAME = "Celebrate 5 years of finding each other on SoundCloud";
    private LegacyPlayerScreen playerScreen;

    @Override
    protected Uri getUri() {
        return TestConsts.FACEBOOK_SOUND_URI;
    }

    public void testFacebookTrackDeeplinkOpensPlayerScreenAndLoadRecommendations() {
        playerScreen = new LegacyPlayerScreen(solo);
        solo.assertActivity(com.soundcloud.android.playback.PlayerActivity.class, DEFAULT_WAIT);
        solo.clickOnView(R.id.pause);
        waiter.expect(playerScreen.trackTitleElement())
                .toHaveText(TRACK_NAME);

        // make sure recommendations load
        playerScreen.swipeLeft();
        assertNotSame(TRACK_NAME, playerScreen.trackTitle());
    }
}
