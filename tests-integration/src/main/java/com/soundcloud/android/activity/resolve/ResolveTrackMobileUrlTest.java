package com.soundcloud.android.activity.resolve;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

import android.net.Uri;

public class ResolveTrackMobileUrlTest extends ResolveBaseTest {

    public void testShouldOpenPlayerScreenAndLoadRecommentations() throws Exception {
        final String expectedTitle = "STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]";
        waiter.waitForContentAndRetryIfLoadingFailed();

        final VisualPlayerElement playerElement = getPlayerElement();
        assertThat(playerElement.getTrackTitle(), is(equalToIgnoringCase(expectedTitle)));
        playerElement.swipeNext();
        assertThat(playerElement.getTrackTitle(), is(not(equalToIgnoringCase(expectedTitle))));
    }

    @Override
    protected Uri getUri() {
        return TestConsts.CHE_FLUTE_M_URI;
    }
}
