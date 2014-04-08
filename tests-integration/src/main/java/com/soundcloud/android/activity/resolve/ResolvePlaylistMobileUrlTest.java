package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.PlaylistDetailsScreen;

import android.net.Uri;
import android.test.suitebuilder.annotation.Suppress;

@Suppress
// suppress as the Resolve endpoint does not support "playlists" yet
public class ResolvePlaylistMobileUrlTest extends ResolveBaseTest {

    public void testShouldOpenPlaylistDetailsScreen () throws Exception {
        PlaylistDetailsScreen pd = new PlaylistDetailsScreen(solo);
        assertEquals("Ecclesia Inspiration", pd.getTitle());
    }

    protected Uri getUri() {
        return TestConsts.FORSS_PLAYLIST_M_URI;
    }
}
