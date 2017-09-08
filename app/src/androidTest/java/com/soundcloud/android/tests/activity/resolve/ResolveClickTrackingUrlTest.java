package com.soundcloud.android.tests.activity.resolve;

import static android.net.Uri.parse;
import static com.soundcloud.android.tests.TestConsts.STEVE_ANGELLO_PERMALINK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.TestConsts;
import org.junit.Test;

import android.net.Uri;

public class ResolveClickTrackingUrlTest extends ResolveBaseTest {
    @Override
    protected Uri getUri() {
        return parse("http://soundcloud.com/-/t/click/postman-email-follower?url=" + STEVE_ANGELLO_PERMALINK);
    }

    @Test
    public void testResolveUrl() throws Exception {
        ProfileScreen profileScreen = new ProfileScreen(solo);
        waiter.waitForActivity(ProfileActivity.class);
        assertThat(profileScreen.getUserName(), equalToIgnoringCase("steveangello"));
    }
}
