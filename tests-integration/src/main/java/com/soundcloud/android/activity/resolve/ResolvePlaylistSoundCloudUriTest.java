package com.soundcloud.android.activity.resolve;

import static com.soundcloud.android.tests.matcher.view.IsVisible.Visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.PlaylistDetailsScreen;

import android.net.Uri;

public class ResolvePlaylistSoundCloudUriTest extends ResolveBaseTest {

    public void testShouldOpenPlaylistDetails() {
        PlaylistDetailsScreen playlistDetailsScreen = new PlaylistDetailsScreen(solo);

        assertThat(playlistDetailsScreen, is(Visible()));
        assertThat(playlistDetailsScreen.getTitle(), is(equalToIgnoringCase("Ecclesia Inspiration")));
    }

    @Override
    protected Uri getUri() {
        return TestConsts.FORSS_PLAYLIST_SC_URI;
    }
}
