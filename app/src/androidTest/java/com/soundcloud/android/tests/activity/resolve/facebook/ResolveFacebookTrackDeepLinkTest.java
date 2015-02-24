package com.soundcloud.android.tests.activity.resolve.facebook;

import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.tests.TestConsts;
import com.soundcloud.android.tests.activity.resolve.ResolveBaseTest;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

import android.net.Uri;

public class ResolveFacebookTrackDeepLinkTest extends ResolveBaseTest {
    @Override
    protected Uri getUri() {
        return TestConsts.FACEBOOK_SOUND_DEEP_LINK;
    }

    public void testShowExpandedPlayerWhenTrackUrnIsValid() {
        assertThat(new VisualPlayerElement(solo), is(expanded()));
    }
}
