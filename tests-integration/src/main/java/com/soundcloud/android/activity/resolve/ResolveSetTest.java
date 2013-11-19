package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.screens.PlayerScreen;

public abstract class ResolveSetTest extends ResolveBaseTest {

    public void testResolveUrl() throws Exception {
        playerScreen = new PlayerScreen(solo);
        solo.assertActivity(PlaylistDetailActivity.class, DEFAULT_WAIT);
        assertEquals("Ecclesia Inspiration", playerScreen.trackTitle());
    }
}