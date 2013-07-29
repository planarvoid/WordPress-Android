package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.activity.WebViewActivity;

import android.net.Uri;

public class ResolveUnhandledUrls extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.JOBS_PAGE;
    }

    public void ignore_testResolveUrlShouldOpenInWebView() throws Exception {
        solo.assertActivity(WebViewActivity.class, DEFAULT_WAIT);
    }
}
