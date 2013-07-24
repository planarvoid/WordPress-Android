package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.activity.track.PlaylistActivity;

public abstract class ResolveSetTest extends ResolveBaseTest {

    public void ignore_testResolveUrl() throws Exception {
        solo.assertActivity(PlaylistActivity.class, DEFAULT_WAIT);
        solo.assertText("Ecclesia Inspiration");
    }
}