package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.PlayerScreen;

import android.net.Uri;

public class ResolveTrackMobileUrlTest extends ResolveBaseTest {
    public void testShouldOpenPlayerScreen() throws Exception {
        playerScreen = new PlayerScreen(solo);
        playerScreen.stopPlayback();

        assertEquals("STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]", playerScreen.trackTitle());
    }

    @Override
    protected Uri getUri() {
        return TestConsts.CHE_FLUTE_M_URI;
    }
}
