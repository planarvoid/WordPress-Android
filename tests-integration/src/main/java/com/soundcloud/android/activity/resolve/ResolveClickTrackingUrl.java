package com.soundcloud.android.activity.resolve;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.ProfileScreen;

import android.net.Uri;

public class ResolveClickTrackingUrl extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return Uri.parse("http://soundcloud.com/-/t/click/postman-email-follower?url="+TestConsts.STEVE_ANGELLO_URI);
    }

    public void testResolveUrl() throws Exception {
        ProfileScreen profileScreen = new ProfileScreen(solo);
        assertThat(profileScreen.getUserName(), equalToIgnoringCase("steveangello"));
    }
}
