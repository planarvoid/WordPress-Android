package com.soundcloud.android.activity.resolve;

import static com.soundcloud.android.tests.matcher.player.IsExpanded.expanded;
import static com.soundcloud.android.tests.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

import android.net.Uri;

public class ResolveTrackLoggedIn extends ResolveBaseTest {
    private VisualPlayerElement visualPlayer;

    @Override
    protected void setUp() throws Exception {
        // Note : ResolveBaseTest launch the Activity with the data provided by getUri()
        super.setUp();
        visualPlayer = new VisualPlayerElement(solo);
    }

    @Override
    protected Uri getUri() {
        return TestConsts.CHE_FLUTE_URI;
    }

    public void testShouldOpenPlayerFromDeeplink() {
        assertThat(new StreamScreen(solo), is(visible()));
        assertThat(visualPlayer, is(expanded()));
        assertThat(visualPlayer.getTrackTitle(), is(equalToIgnoringCase("STEVE ANGELLO - CHE FLUTE [FREE SIZE DOWNLOAD]")));
        // Assert track is playing
    }
}
