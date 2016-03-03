package com.soundcloud.android.tests.activity.resolve.facebook;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class ResolveFacebookUserPermalinkTest extends FacebookResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.FACEBOOK_USER_PERMALINK;
    }

    public void testFacebookUserDeeplink() {
        final ProfileScreen profileScreen = new ProfileScreen(solo);
        assertThat(profileScreen.getUserName(), is(equalToIgnoringCase("steveangello")));
    }
}
