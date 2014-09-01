package com.soundcloud.android.activity.resolve;

import static com.soundcloud.android.tests.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.ProfileScreen;

import android.net.Uri;

public class ResolveUserSoundCloudUriTest extends ResolveBaseTest {
    @Override
    protected Uri getUri() {
        return TestConsts.STEVE_ANGELLO_SC_URI;
    }

    public void testResolveUrl() {
        ProfileScreen profileScreen = new ProfileScreen(solo);
        assertThat(profileScreen, is(visible()));
        assertThat(profileScreen.getUserName(), is(equalToIgnoringWhiteSpace("steveangello")));
    }
}