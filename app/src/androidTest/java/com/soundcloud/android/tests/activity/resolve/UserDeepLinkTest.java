package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.tests.TestConsts.UNRESOLVABLE_USER_DEEPLINK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.tests.TestConsts;
import org.junit.Test;

import android.net.Uri;

public class UserDeepLinkTest extends ResolveBaseTest {
    @Override
    protected Uri getUri() {
        return UNRESOLVABLE_USER_DEEPLINK;
    }

    @Test
    public void testResolveUserActivityResolved() throws Exception {
        waiter.waitForActivity(ProfileActivity.class);
        assertThat(solo.getCurrentActivity().getClass().getSimpleName(), is(ProfileActivity.class.getSimpleName()));
    }
}
