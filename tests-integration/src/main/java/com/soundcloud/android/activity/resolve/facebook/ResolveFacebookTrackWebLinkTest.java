package com.soundcloud.android.activity.resolve.facebook;

import static com.soundcloud.android.tests.hamcrest.IsVisible.Visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.R;
import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.LegacyPlayerScreen;

import android.net.Uri;

public class ResolveFacebookTrackWebLinkTest extends FacebookResolveBaseTest {

    private static final String TRACK_NAME = "STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]";
    private LegacyPlayerScreen playerScreen;

    @Override
    protected Uri getUri() {
        return TestConsts.FACEBOOK_SOUND_URI;
    }

    public void testFacebookTrackDeeplinkOpensPlayerScreenAndLoadRecommendations() {
        playerScreen = new LegacyPlayerScreen(solo);
        assertThat(playerScreen, is(Visible()));

        playerScreen.stopPlayback();
        waiter.expect(playerScreen.trackTitleElement())
                .toHaveText(TRACK_NAME);

        // make sure recommendations load
        playerScreen.swipeLeft();

        assertThat(TRACK_NAME, is(not(equalToIgnoringCase(playerScreen.trackTitle()))));
    }
}
