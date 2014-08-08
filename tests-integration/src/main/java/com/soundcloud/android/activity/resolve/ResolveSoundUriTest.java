package com.soundcloud.android.activity.resolve;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

import android.net.Uri;


public class ResolveSoundUriTest extends ResolveBaseTest {

    private static final String TRACK_NAME = "STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]";

    public void testShouldOpenPlayerScreenAndLoadRecommentations() {
        VisualPlayerElement playerScreen = new VisualPlayerElement(solo);

        assertThat(playerScreen.getTrackTitle(), is(equalTo(TRACK_NAME)));

        // make sure recommendations load
        waiter.waitForNextTrack();

        playerScreen.swipeNext();
        assertThat(TRACK_NAME, is(not(equalTo(playerScreen.getTrackTitle()))));
    }

    @Override
    protected Uri getUri() {
        return TestConsts.FORSS_SOUND_URI;
    }
}
