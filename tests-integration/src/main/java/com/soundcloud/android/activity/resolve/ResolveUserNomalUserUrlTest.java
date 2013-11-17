package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.profile.ProfileActivity;

import android.net.Uri;

public class ResolveUserNomalUserUrlTest extends ResolveBaseTest {
    @Override
    protected Uri getUri() {
        return TestConsts.STEVE_ANGELLO_URI;
    }

    public void testResolveUrl() throws Exception {
        solo.assertActivity(ProfileActivity.class, DEFAULT_WAIT);
        // TODO: Use POMs here
        solo.assertText("steveangello");
    }
}
