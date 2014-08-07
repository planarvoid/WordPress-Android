package com.soundcloud.android.activity.resolve.facebook;

import static com.soundcloud.android.tests.matcher.view.IsVisible.Visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

import android.net.Uri;

public class ResolveFacebookTrackWebLinkTest extends FacebookResolveBaseTest {

    private static final String TRACK_NAME = "STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]";

    @Override
    protected Uri getUri() {
        return TestConsts.FACEBOOK_SOUND_URI;
    }

    public void testFacebookTrackDeeplinkOpensPlayerScreenAndLoadRecommendations() {
        VisualPlayerElement player = new VisualPlayerElement(solo);
        player.waitForExpandedPlayer();
        player.waitForContent();

        assertThat(player.isVisible(), is(true));
        assertThat(player.getTrackTitle(), is(TRACK_NAME));
        assertThat(player, is(Visible()));

        player.swipeNext();
        assertThat(player.getTrackTitle(), is(not(TRACK_NAME)));
    }
}
