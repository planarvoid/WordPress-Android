package com.soundcloud.android.activity.resolve;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.screens.LegacyPlayerScreen;

import android.net.Uri;


public class ResolveSoundUriTest extends ResolveBaseTest {

    private static final String TRACK_NAME = "STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]";

    public void testShouldOpenPlayerScreenAndLoadRecommentations() throws Exception {
        // TODO : no recommendation for the visual player ??
        if (featureFlags.isDisabled(Feature.VISUAL_PLAYER)) {
            LegacyPlayerScreen playerScreen = new LegacyPlayerScreen(solo);
            playerScreen.stopPlayback();

            waiter.expect(playerScreen.trackTitle())
                    .toHaveText(TRACK_NAME);

            // make sure recommendations load
            playerScreen.swipeLeft();
            assertThat(TRACK_NAME, is(not(equalTo(playerScreen.getTrackTitle()))));
        }
    }

    @Override
    protected Uri getUri() {
        return TestConsts.FORSS_SOUND_URI;
    }
}
