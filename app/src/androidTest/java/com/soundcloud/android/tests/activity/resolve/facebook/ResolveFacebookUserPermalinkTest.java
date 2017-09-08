package com.soundcloud.android.tests.activity.resolve.facebook;

import static com.soundcloud.android.tests.TestConsts.FACEBOOK_USER_PERMALINK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.TestConsts;
import org.junit.Test;

import android.net.Uri;

public class ResolveFacebookUserPermalinkTest extends FacebookResolveBaseTest {

    @Override
    protected Uri getUri() {
        return FACEBOOK_USER_PERMALINK;
    }

    @Test
    public void testFacebookUserDeeplink() throws Exception {
        final ProfileScreen profileScreen = new ProfileScreen(solo);
        assertThat(profileScreen.getUserName(), is(equalToIgnoringCase("steveangello")));
    }
}
