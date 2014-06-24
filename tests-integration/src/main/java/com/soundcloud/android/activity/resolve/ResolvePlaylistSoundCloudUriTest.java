package com.soundcloud.android.activity.resolve;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.screens.PlaylistDetailsScreen;

import android.net.Uri;

public class ResolvePlaylistSoundCloudUriTest extends ResolveBaseTest {

    public void testShouldOpenPlaylistDetails() throws Exception {
        if (featureFlags.isEnabled(Feature.VISUAL_PLAYER)) {
            assertThat(getPlayerElement().getTrackTitle(), is(equalToIgnoringCase("Duruflé  Introit and Kyrie")));
        } else {
            assertThat(new PlaylistDetailsScreen(solo).getTitle(), is(equalToIgnoringCase("Ecclesia Inspiration")));
        }
    }

    @Override
    protected Uri getUri() {
        return TestConsts.FORSS_PLAYLIST_SC_URI;
    }
}
