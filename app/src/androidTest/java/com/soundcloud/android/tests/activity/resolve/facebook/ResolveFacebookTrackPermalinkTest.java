package com.soundcloud.android.tests.activity.resolve.facebook;

import static com.soundcloud.android.framework.matcher.player.IsExpanded.expanded;
import static com.soundcloud.android.tests.TestConsts.FACEBOOK_TRACK_PERMALINK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.TestConsts;
import org.junit.Test;

import android.net.Uri;

public class ResolveFacebookTrackPermalinkTest extends FacebookResolveBaseTest {

    private static final String TRACK_NAME = "STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]";

    @Override
    protected Uri getUri() {
        return FACEBOOK_TRACK_PERMALINK;
    }

    @Test
    public void testFacebookTrackDeeplinkOpensPlayerScreenAndLoadRecommendations() throws Exception {
        VisualPlayerElement player = new VisualPlayerElement(solo);
        player.waitForContent();
        assertThat(player, is(expanded()));
        assertThat(player.getTrackTitle(), is(TRACK_NAME));
        player.waitForPlayState();
        player.waitForMoreContent();
        assertThat(player.hasMoreTracks(), is(true));
    }
}
