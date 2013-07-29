package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.activity.UserBrowser;

import android.net.Uri;
import android.test.suitebuilder.annotation.Suppress;

@Suppress
// XXX suppressed because of limitation of Android's activity monitoring
public class ResolveUserSoundCloudUriTest extends ResolveBaseTest {
    @Override
    protected Uri getUri() {
        return TestConsts.STEVE_ANGELLO_SC_URI;
    }

    public void ignore_testResolveUrl() throws Exception {
        solo.assertActivity(UserBrowser.class, DEFAULT_WAIT);
        solo.assertText("steveangello");
    }
}