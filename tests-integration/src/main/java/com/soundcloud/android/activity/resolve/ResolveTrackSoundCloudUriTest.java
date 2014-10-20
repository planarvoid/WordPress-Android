package com.soundcloud.android.activity.resolve;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

import android.net.Uri;

public class ResolveTrackSoundCloudUriTest extends ResolveBaseTest {

    private static final String TRACK_NAME = "STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]";

    public void testShouldOpenPlayerScreenAndLoadRecommendations() {
        final VisualPlayerElement playerElement = getPlayerElement();
        assertThat(playerElement.getTrackTitle(), is(equalToIgnoringCase(TRACK_NAME)));
        // make sure recommendations load
        waiter.waitFiveSeconds();

        playerElement.swipeNext();
        assertThat(playerElement.getTrackTitle(), is(not(equalToIgnoringCase(TRACK_NAME))));
    }

    @Override
    protected Uri getUri() {
        return TestConsts.CHE_FLUTE_SC_URI;
    }
}
