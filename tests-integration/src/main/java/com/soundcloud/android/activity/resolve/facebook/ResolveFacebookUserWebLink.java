package com.soundcloud.android.activity.resolve.facebook;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.ProfileScreen;

import android.net.Uri;

public class ResolveFacebookUserWebLink extends FacebookResolveBaseTest {

    private ProfileScreen profileScreen;

    @Override
    protected Uri getUri() {
        return TestConsts.FACEBOOK_USER_URI;
    }

    public void testFacebookUserDeeplink() {
        profileScreen = new ProfileScreen(solo);
        assertThat(profileScreen.getUserName(), is(equalToIgnoringCase("steveangello")));
    }
}
