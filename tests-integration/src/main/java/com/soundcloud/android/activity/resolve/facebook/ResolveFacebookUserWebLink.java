package com.soundcloud.android.activity.resolve.facebook;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.profile.ProfileActivity;

import android.net.Uri;

public class ResolveFacebookUserWebLink extends FacebookResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.FACEBOOK_USER_URI;
    }

    public void testFacebookUserDeeplink() {
        solo.assertActivity(ProfileActivity.class, DEFAULT_WAIT);
        solo.assertText("steveangello");
    }
}
