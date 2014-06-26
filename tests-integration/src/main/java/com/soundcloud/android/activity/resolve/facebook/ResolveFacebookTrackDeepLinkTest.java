package com.soundcloud.android.activity.resolve.facebook;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.activity.resolve.ResolveBaseTest;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

import android.net.Uri;

public class ResolveFacebookTrackDeepLinkTest extends ResolveBaseTest {
    @Override
    protected Uri getUri() {
        return TestConsts.FACEBOOK_SOUND_DEEP_LINK;
    }

    public void testStartPlayerActivityWhenTrackUrnIsValid() throws Exception {
        if (featureFlags.isEnabled(Feature.VISUAL_PLAYER)) {
            VisualPlayerElement player = new VisualPlayerElement(solo);
            assertThat(player.isVisible(), is(true));
        } else {
            solo.assertActivity(com.soundcloud.android.playback.PlayerActivity.class, DEFAULT_WAIT);
        }
    }
}
