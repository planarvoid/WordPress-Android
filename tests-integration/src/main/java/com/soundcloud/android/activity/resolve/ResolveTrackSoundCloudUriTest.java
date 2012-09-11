package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.TestConsts;

import android.net.Uri;
import android.test.suitebuilder.annotation.Suppress;

@Suppress
// XXX suppressed because of limitation of Android's activity monitoring
// Main starts ScPlayer but ScPlayer doesn't show up in the activitystack
// seems to be a timing issue
public class ResolveTrackSoundCloudUriTest extends ResolveTrackTest {
    @Override
    protected Uri getUri() {
        return TestConsts.CHE_FLUTE_SC_URI;
    }
}
