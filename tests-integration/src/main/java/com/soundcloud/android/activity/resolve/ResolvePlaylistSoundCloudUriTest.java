package com.soundcloud.android.activity.resolve;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.TestConsts;

import android.net.Uri;

public class ResolvePlaylistSoundCloudUriTest extends ResolveBaseTest {

    public void testShouldOpenPlaylistDetails() throws Exception {
        assertThat(getPlayerElement().getTrackTitle(), is(equalToIgnoringCase("Duruflé  Introit and Kyrie")));
    }

    @Override
    protected Uri getUri() {
        return TestConsts.FORSS_PLAYLIST_SC_URI;
    }
}
