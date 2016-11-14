package com.soundcloud.android.tests.activity.resolve;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class ResolveTrackMobileUrlTest extends ResolveBaseTest {

    public void testShouldOpenPlayerScreenAndLoadRecommentations() {
        final String expectedTitle = "STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]";
        waiter.waitForContentAndRetryIfLoadingFailed();

        final VisualPlayerElement playerElement = getPlayerElement();
        assertThat(playerElement.getTrackTitle(), is(equalToIgnoringCase(expectedTitle)));

        playerElement.waitForMoreContent(); // wait for similar sounds to be loaded
        assertThat(playerElement.hasMoreTracks(), is(true));
    }

    @Override
    protected Uri getUri() {
        return TestConsts.CHE_FLUTE_M_URI;
    }
}