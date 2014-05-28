package com.soundcloud.android.activity.resolve;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.profile.ProfileActivity;

import android.net.Uri;
import com.soundcloud.android.screens.ProfileScreen;

public class ResolveClickTrackingUrl extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return Uri.parse("http://soundcloud.com/-/t/click/postman-email-follower?url="+TestConsts.STEVE_ANGELLO_URI);
    }

    public void testResolveUrl() throws Exception {
        profileScreen = new ProfileScreen(solo);
        solo.assertActivity(ProfileActivity.class, DEFAULT_WAIT);
        assertThat(profileScreen.userName(), equalToIgnoringCase("steveangello"));
    }
}
