package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.R;
import com.soundcloud.android.playback.PlayerActivity;

public abstract class ResolveTrackTest extends ResolveBaseTest {

    public void testResolveUrl() throws Exception {
        solo.assertActivity(PlayerActivity.class, DEFAULT_WAIT);
        waiter.waitForPlayerPlaying();
        assertEquals("STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]", playerScreen.trackTitle());

        solo.clickOnView(R.id.pause);
    }
}