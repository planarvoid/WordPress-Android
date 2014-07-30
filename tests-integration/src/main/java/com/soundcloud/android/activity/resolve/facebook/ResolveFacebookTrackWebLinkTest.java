package com.soundcloud.android.activity.resolve.facebook;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

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
        assertThat(player.isVisible(), is(true));
        assertThat(player.getTrackTitle(), is(equalToIgnoringCase(TRACK_NAME)));
        // TODO : No recommendation for the visual player ?
//            playerScreen = new LegacyPlayerScreen(solo);
//            assertThat(playerScreen, is(Visible()));
//
//            playerScreen.stopPlayback();
//            waiter.expect(playerScreen.trackTitle())
//                    .toHaveText(TRACK_NAME);
//
//            // make sure recommendations load
//            playerScreen.swipeLeft();
//
//            assertThat(TRACK_NAME, is(not(equalToIgnoringCase(playerScreen.getTrackTitle()))));

    }
}
