package com.soundcloud.android.tests.activity.resolve;

import android.net.Uri;

import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.TestConsts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ResolvePrivateTrackTest extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.PRIVATE_SHARED_TRACK;
    }

    public void bug_3205_testShouldOpenPrivatelySharedTrack() {
        final VisualPlayerElement playerElement = getPlayerElement();
        assertThat("Player should be playing a track", playerElement.isExpandedPlayerPlaying(), is(true));
    }
}
