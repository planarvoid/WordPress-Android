package com.soundcloud.android.tests.activity.resolve;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class UserDeepLinkTest extends ResolveBaseTest {
    @Override
    protected Uri getUri() {
        return TestConsts.UNRESOLVABLE_USER_DEEPLINK;
    }

    public void testResolveUserActivityResolved() {
        waiter.waitForActivity(ProfileActivity.class);
        assertThat(solo.getCurrentActivity().getClass().getSimpleName(), is(ProfileActivity.class.getSimpleName()));
    }
}
