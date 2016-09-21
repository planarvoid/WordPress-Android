package com.soundcloud.android.tests.activity.resolve;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class ResolveClickTrackingUrlTest extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return Uri.parse("http://soundcloud.com/-/t/click/postman-email-follower?url=" + TestConsts.STEVE_ANGELLO_PERMALINK);
    }

    public void testResolveUrl() {
        ProfileScreen profileScreen = new ProfileScreen(solo);
        waiter.waitForActivity(ProfileActivity.class);
        assertThat(profileScreen.getUserName(), equalToIgnoringCase("steveangello"));
    }
}