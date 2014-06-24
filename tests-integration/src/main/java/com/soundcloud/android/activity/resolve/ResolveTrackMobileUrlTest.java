package com.soundcloud.android.activity.resolve;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.screens.LegacyPlayerScreen;
import com.soundcloud.android.screens.elements.PlayerElement;

import android.net.Uri;

public class ResolveTrackMobileUrlTest extends ResolveBaseTest {

    public void testShouldOpenPlayerScreenAndLoadRecommentations() throws Exception {
        // TODO : We don't have recommendations for the visual player ?
        if (featureFlags.isEnabled(Feature.VISUAL_PLAYER) == false) {
            final String expectedTitle = "STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]";
            waiter.waitForContentAndRetryIfLoadingFailed();
            // trick (?)
            new LegacyPlayerScreen(solo).stopPlayback();
            final PlayerElement playerElement = getPlayerElement();
            assertThat(playerElement.getTrackTitle(), is(equalToIgnoringCase(expectedTitle)));
            playerElement.swipeNext();
            assertThat(playerElement.getTrackTitle(), is(not(equalToIgnoringCase(expectedTitle))));
        }
    }

    @Override
    protected Uri getUri() {
        return TestConsts.CHE_FLUTE_M_URI;
    }
}
