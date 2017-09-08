package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.tests.TestConsts.STEVE_ANGELLO_DEEP_LINK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.annotation.Ignore;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.tests.TestConsts;
import org.junit.Test;

import android.net.Uri;

public class ResolveGoogleCrawlerProfileTest extends ResolveGoogleCrawlerBaseTest {
    @Override
    protected Uri getUri() {
        return STEVE_ANGELLO_DEEP_LINK;
    }

    @Ignore //FIXME https://soundcloud.atlassian.net/browse/DROID-1351
    @Test
    public void testResolveUrl() throws Exception {
        ProfileScreen profileScreen = new ProfileScreen(solo);
        assertThat(profileScreen, is(visible()));
        assertThat(profileScreen.getUserName(), is(equalToIgnoringWhiteSpace("steveangello")));
    }
}
