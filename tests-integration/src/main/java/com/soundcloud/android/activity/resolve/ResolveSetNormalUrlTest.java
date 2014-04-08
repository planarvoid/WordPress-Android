package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.PlaylistDetailsScreen;

import android.net.Uri;

public class ResolveSetNormalUrlTest extends ResolveBaseTest {

    public void testShouldOpenPlaylistDetails() throws Exception {
        PlaylistDetailsScreen pd = new PlaylistDetailsScreen(solo);
        assertEquals("Ecclesia Inspiration", pd.getTitle());
    }

    @Override
    protected Uri getUri() {
        return TestConsts.FORSS_SET_URI;
    }
}
