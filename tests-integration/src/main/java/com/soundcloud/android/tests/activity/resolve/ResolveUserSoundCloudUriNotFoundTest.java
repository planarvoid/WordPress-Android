package com.soundcloud.android.tests.activity.resolve;

import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class ResolveUserSoundCloudUriNotFoundTest extends ResolveBaseTest {
    @Override
    protected Uri getUri() {
        return TestConsts.UNRESOLVABLE_SC_USER_URI;
    }

    public void testResolveUnknownUrlShouldShowErrorLoadingUrl() {
        assertTrue(waiter.expectToastWithText(toastObserver, "There was a problem loading that url"));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }
}
