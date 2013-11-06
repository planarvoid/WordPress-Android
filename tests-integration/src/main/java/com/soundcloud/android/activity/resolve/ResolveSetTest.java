package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.activity.track.PlaylistDetailActivity;

public abstract class ResolveSetTest extends ResolveBaseTest {

    public void testResolveUrl() throws Exception {
        solo.assertActivity(PlaylistDetailActivity.class, DEFAULT_WAIT);
        assertEquals("Ecclesia Inspiration", playerScreen.trackTitle());
    }
}