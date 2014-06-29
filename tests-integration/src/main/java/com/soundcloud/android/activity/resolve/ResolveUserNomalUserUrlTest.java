package com.soundcloud.android.activity.resolve;

import static com.soundcloud.android.tests.hamcrest.IsVisible.Visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.ProfileScreen;

import android.net.Uri;

public class ResolveUserNomalUserUrlTest extends ResolveBaseTest {
    @Override
    protected Uri getUri() {
        return TestConsts.STEVE_ANGELLO_URI;
    }

    public void testResolveUrl() throws Exception {
        ProfileScreen profileScreen = new ProfileScreen(solo);
        assertThat(profileScreen, is(Visible()));
        assertThat(profileScreen.getUserName(), is(equalToIgnoringWhiteSpace("steveangello")));
    }
}
