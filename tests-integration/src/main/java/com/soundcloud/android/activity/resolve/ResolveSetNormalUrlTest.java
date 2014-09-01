package com.soundcloud.android.activity.resolve;

import static com.soundcloud.android.tests.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.PlaylistDetailsScreen;

import android.net.Uri;

public class ResolveSetNormalUrlTest extends ResolveBaseTest {

    public void testShouldOpenPlaylistDetails() {
        PlaylistDetailsScreen playlistDetailsScreen = new PlaylistDetailsScreen(solo);

        assertThat(playlistDetailsScreen, is(visible()));
        assertThat(playlistDetailsScreen.getTitle(), is(equalToIgnoringCase("Ecclesia Inspiration")));
    }

    @Override
    protected Uri getUri() {
        return TestConsts.FORSS_SET_URI;
    }
}
