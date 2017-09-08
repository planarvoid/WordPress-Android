package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.tests.TestConsts.CHE_FLUTE_TRACK_PERMALINK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.TestConsts;
import org.junit.Test;

import android.net.Uri;

public class ResolveTrackPermalinkTest extends ResolveBaseTest {

    private static final String TRACK_NAME = "STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]";

    @Test
    public void testShouldOpenPlayerScreenAndLoadRecommendations() throws Exception {
        final VisualPlayerElement playerElement = getPlayerElement();
        assertThat(playerElement.getTrackTitle(), is(equalToIgnoringCase(TRACK_NAME)));
        // make sure recommendations load
        playerElement.waitForMoreContent();
        assertThat(playerElement.hasMoreTracks(), is(true));
    }

    @Override
    protected Uri getUri() {
        return CHE_FLUTE_TRACK_PERMALINK;
    }
}
