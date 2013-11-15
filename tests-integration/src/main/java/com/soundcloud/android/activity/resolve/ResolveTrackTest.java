package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.PlayerActivity;

public abstract class ResolveTrackTest extends ResolveBaseTest {

    public void testResolveUrl() throws Exception {
        solo.assertActivity(PlayerActivity.class, DEFAULT_WAIT);
        waiter.waitForPlayerPlaying();
        assertEquals("STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]", playerScreen.trackTitle());

        // make sure track doesn't keep playing in the background
        solo.clickOnView(R.id.pause);
    }
}