package com.soundcloud.android.activity.resolve;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.TestConsts;

import android.net.Uri;

public class ResolveUserSoundCloudUriNotFoundTest extends ResolveBaseTest {
    @Override
    protected Uri getUri() {
        return TestConsts.UNRESOLVABLE_SC_USER_URI;
    }

    public void testResolveUnknownUrlShouldShowErrorLoadingUrl() throws Exception {
        waiter.expectToast().toHaveText("There was a problem loading that url");
    }
}
