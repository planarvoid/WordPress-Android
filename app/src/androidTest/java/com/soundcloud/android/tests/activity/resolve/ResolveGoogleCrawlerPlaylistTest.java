package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class ResolveGoogleCrawlerPlaylistTest extends ResolveGoogleCrawlerBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.FORSS_PLAYLIST_SC_URI;
    }

    public void testResolveUrl() {
        PlaylistDetailsScreen playlistScreen = new PlaylistDetailsScreen(solo);
        assertThat(playlistScreen, is(visible()));
        assertThat(playlistScreen.getTitle(), is(equalToIgnoringCase("Ecclesia Inspiration")));
    }
}
