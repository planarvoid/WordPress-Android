package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.PlayerScreen;

import android.net.Uri;

public class ResolveTrackNormalUrlTest extends ResolveBaseTest {

    public void testShouldOpenPlayerScreen() throws Exception {
        playerScreen = new PlayerScreen(solo);
        playerScreen.stopPlayback();

        waiter.expect(playerScreen.trackTitleElement())
                .toHaveText("STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]");
    }

    @Override
    protected Uri getUri() {
        return TestConsts.CHE_FLUTE_URI;
    }
}
