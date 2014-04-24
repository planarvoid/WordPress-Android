package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.profile.ProfileActivity;

import android.net.Uri;
import android.test.suitebuilder.annotation.Suppress;

public class ResolveUserSoundCloudUriTest extends ResolveBaseTest {
    @Override
    protected Uri getUri() {
        return TestConsts.STEVE_ANGELLO_SC_URI;
    }

    public void ignoretestResolveUrl() throws Exception {
        solo.assertActivity(ProfileActivity.class, DEFAULT_WAIT);
        // TODO: Use POMs here
        solo.assertText("steveangello");
    }
}