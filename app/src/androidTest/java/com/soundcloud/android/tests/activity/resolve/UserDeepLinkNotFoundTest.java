package com.soundcloud.android.tests.activity.resolve;

import com.soundcloud.android.R;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class UserDeepLinkNotFoundTest extends ResolveBaseTest {
    @Override
    protected Uri getUri() {
        return TestConsts.UNRESOLVABLE_USER_DEEPLINK;
    }

    public void testResolveUnknownUrlShouldShowErrorLoadingUrl() {
        assertTrue(waiter.expectToastWithText(toastObserver, resourceString(R.string.error_loading_url)));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }
}
