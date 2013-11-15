package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.TestConsts;

import android.net.Uri;
import android.test.suitebuilder.annotation.Suppress;

@Suppress
// suppress as the Resolve endpoint does not support "playlists" yet
public class ResolvePlaylistNormalUrlTest extends ResolveSetTest {
    @Override
    protected Uri getUri() {
        return TestConsts.FORSS_PLAYLIST_URI;
    }
}
