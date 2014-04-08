package com.soundcloud.android.activity.resolve;

import android.net.Uri;
import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.PlayerScreen;


public class ResolveSoundUriTest extends ResolveBaseTest {

    public void testShouldOpenPlayerScreen() throws Exception {
        playerScreen = new PlayerScreen(solo);
        playerScreen.stopPlayback();

        assertEquals("STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]", playerScreen.trackTitle());
    }

    @Override
    protected Uri getUri() {
        return TestConsts.FORSS_SOUND_URI;
    }
}
