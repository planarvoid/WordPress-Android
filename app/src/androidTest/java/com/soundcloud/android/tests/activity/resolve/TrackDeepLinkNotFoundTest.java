package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.R.string;
import static com.soundcloud.android.R.string.error_unknown_navigation;
import static com.soundcloud.android.tests.TestConsts.UNRESOLVABLE_TRACK_DEEPLINK;
import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.R;
import com.soundcloud.android.tests.TestConsts;
import org.junit.Test;

import android.net.Uri;

public class TrackDeepLinkNotFoundTest extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return UNRESOLVABLE_TRACK_DEEPLINK;
    }

    @Test
    public void testResolveUnknownUrlShouldShowErrorLoadingUrl() throws Exception {
        assertTrue(waiter.expectToastWithText(toastObserver, resourceString(error_unknown_navigation)));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }
}
