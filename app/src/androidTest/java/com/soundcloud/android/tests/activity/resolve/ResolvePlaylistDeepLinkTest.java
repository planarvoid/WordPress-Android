package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class ResolvePlaylistDeepLinkTest extends ResolveBaseTest {

    public void testShouldOpenPlaylistDetails() {
        PlaylistDetailsScreen playlistDetailsScreen = new PlaylistDetailsScreen(solo);

        assertThat(playlistDetailsScreen, is(visible()));
        assertThat(playlistDetailsScreen.getTitle(), is(equalToIgnoringCase("Ecclesia Inspiration")));
    }

    @Override
    protected Uri getUri() {
        return TestConsts.FORSS_PLAYLIST_DEEP_LINK;
    }
}
