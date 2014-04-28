package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.PlayerScreen;

import android.net.Uri;

public class ResolveTrackMobileUrlTest extends ResolveBaseTest {

    private static final String TRACK_NAME = "STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]";

    public void testShouldOpenPlayerScreenAndLoadRecommentations() throws Exception {
        playerScreen = new PlayerScreen(solo);
        playerScreen.stopPlayback();

        waiter.expect(playerScreen.trackTitleElement())
                .toHaveText(TRACK_NAME);

        // make sure recommendations load
        playerScreen.swipeLeft();
        assertNotSame(TRACK_NAME, playerScreen.trackTitle());
    }

    @Override
    protected Uri getUri() {
        return TestConsts.CHE_FLUTE_M_URI;
    }
}
