package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.TestConsts;

import android.net.Uri;

public class ResolveTrackSoundCloudUriTest extends ResolveTrackTest {
    @Override
    protected Uri getUri() {
        return TestConsts.CHE_FLUTE_SC_URI;
    }
}
