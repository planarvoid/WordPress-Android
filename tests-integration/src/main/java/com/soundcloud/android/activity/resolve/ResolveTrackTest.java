package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.screens.PlayerScreen;

public abstract class ResolveTrackTest extends ResolveBaseTest {

    public void testResolveUrl() throws Exception {
        playerScreen = new PlayerScreen(solo);
        playerScreen.stopPlayback();

        assertEquals("STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]", playerScreen.trackTitle());
    }
}