package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.tests.TestConsts.FORSS_PLAYLIST_DEEP_LINK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import org.junit.Test;

import android.net.Uri;

public class ResolveGoogleCrawlerPlaylistTest extends ResolveGoogleCrawlerBaseTest {

    @Override
    protected Uri getUri() {
        return FORSS_PLAYLIST_DEEP_LINK;
    }

    @Test
    @Ignore //FIXME https://soundcloud.atlassian.net/browse/DROID-1351
    public void testResolveUrl() throws Exception {
        PlaylistDetailsScreen playlistScreen = new PlaylistDetailsScreen(solo);
        assertThat(playlistScreen, is(visible()));
        assertThat(playlistScreen.getTitle(), is(equalToIgnoringCase("Ecclesia Inspiration")));
    }
}
