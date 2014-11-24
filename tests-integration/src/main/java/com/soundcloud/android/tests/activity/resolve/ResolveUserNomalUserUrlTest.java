package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.tests.TestConsts;
import com.soundcloud.android.screens.ProfileScreen;

import android.net.Uri;

public class ResolveUserNomalUserUrlTest extends ResolveBaseTest {
    @Override
    protected Uri getUri() {
        return TestConsts.STEVE_ANGELLO_URI;
    }

    public void testResolveUrl() {
        ProfileScreen profileScreen = new ProfileScreen(solo);
        assertThat(profileScreen, is(visible()));
        assertThat(profileScreen.getUserName(), is(equalToIgnoringWhiteSpace("steveangello")));
    }
}
