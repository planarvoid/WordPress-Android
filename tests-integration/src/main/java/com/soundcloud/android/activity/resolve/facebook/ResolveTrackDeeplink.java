package com.soundcloud.android.activity.resolve.facebook;

import com.soundcloud.android.R;
import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.PlayerScreen;

import android.net.Uri;

public class ResolveTrackDeeplink extends FacebookResolveBaseTest {

    private PlayerScreen playerScreen;

    @Override
    protected Uri getUri() {
        return TestConsts.FACEBOOK_SOUND_URI;
    }

    public void testFacebookTrackDeeplink() {
        playerScreen = new PlayerScreen(solo);
        solo.assertActivity(com.soundcloud.android.playback.PlayerActivity.class, DEFAULT_WAIT);
        solo.clickOnView(R.id.pause);
        waiter.expect(playerScreen.trackTitleElement())
                .toHaveText("Celebrate 5 years of finding each other on SoundCloud");
    }
}
