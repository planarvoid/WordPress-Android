package com.soundcloud.android.tests.activity.resolve;

import com.soundcloud.android.R;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class ResolveTrackSoundCloudUriNotFoundTest extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.UNRESOLVABLE_SC_TRACK_URI;
    }

    public void testResolveUnknownUrlShouldShowErrorLoadingUrl() {
        assertTrue(waiter.expectToastWithText(toastObserver, ressourceString(R.string.error_loading_url)));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }
}
