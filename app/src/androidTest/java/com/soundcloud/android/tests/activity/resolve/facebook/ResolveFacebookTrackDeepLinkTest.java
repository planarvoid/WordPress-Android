package com.soundcloud.android.tests.activity.resolve.facebook;

import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static com.soundcloud.android.tests.TestConsts.FACEBOOK_TRACK_DEEP_LINK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.TestConsts;
import com.soundcloud.android.tests.activity.resolve.ResolveBaseTest;
import org.junit.Test;

import android.net.Uri;

public class ResolveFacebookTrackDeepLinkTest extends ResolveBaseTest {
    @Override
    protected Uri getUri() {
        return FACEBOOK_TRACK_DEEP_LINK;
    }

    @Test
    public void testShowExpandedPlayerWhenTrackUrnIsValid() throws Exception {
        assertThat(new VisualPlayerElement(solo), is(expanded()));
    }
}
