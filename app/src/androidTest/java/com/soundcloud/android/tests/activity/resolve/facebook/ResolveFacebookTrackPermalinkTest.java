package com.soundcloud.android.tests.activity.resolve.facebook;

import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class ResolveFacebookTrackPermalinkTest extends FacebookResolveBaseTest {

    private static final String TRACK_NAME = "STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]";

    @Override
    protected Uri getUri() {
        return TestConsts.FACEBOOK_TRACK_PERMALINK;
    }

    public void testFacebookTrackDeeplinkOpensPlayerScreenAndLoadRecommendations() {
        VisualPlayerElement player = new VisualPlayerElement(solo);
        player.waitForContent();
        assertThat(player, is(expanded()));
        assertThat(player.getTrackTitle(), is(TRACK_NAME));
        player.waitForPlayState();
        player.waitForMoreContent();
        assertThat(player.hasMoreTracks(), is(true));
    }
}
