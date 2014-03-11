package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.screens.PlaylistDetailsScreen;

public abstract class ResolveSetTest extends ResolveBaseTest {

    public void testResolveUrl() throws Exception {
        PlaylistDetailsScreen pd = new PlaylistDetailsScreen(solo);
        assertEquals("Ecclesia Inspiration", pd.getTitle());
    }
}