package com.soundcloud.android.tests.activity.resolve;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class ResolveTwitterUriTest extends ResolveBaseTest {

    private static final String TRACK_NAME = "Tycho - From Home";

    @Override
    protected Uri getUri() {
        return TestConsts.TWITTER_SOUND_URI;
    }

    public void testTwitterUrl() {
        final VisualPlayerElement playerElement = getPlayerElement();
        assertThat(playerElement.getTrackTitle(), is(equalToIgnoringCase(TRACK_NAME)));
        // make sure recommendations load
        waiter.waitFiveSeconds();

        playerElement.swipeNext();
        assertThat(playerElement.getTrackTitle(), is(not(equalToIgnoringCase(TRACK_NAME))));
    }
}
