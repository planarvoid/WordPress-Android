package com.soundcloud.android.activity.resolve;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.LegacyPlayerScreen;

import android.net.Uri;

public class ResolveTrackNormalUrlTest extends ResolveBaseTest {

    private static final String TRACK_NAME = "STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]";

    public void testShouldOpenPlayerScreenAndLoadRecommendations() throws Exception {
        playerScreen = new LegacyPlayerScreen(solo);
        playerScreen.stopPlayback();

        waiter.expect(playerScreen.trackTitle())
                .toHaveText(TRACK_NAME);

        // make sure recommendations load
        playerScreen.swipeLeft();
        assertThat(TRACK_NAME, is(not(equalTo(playerScreen.getTrackTitle()))));
    }

    @Override
    protected Uri getUri() {
        return TestConsts.CHE_FLUTE_URI;
    }
}
