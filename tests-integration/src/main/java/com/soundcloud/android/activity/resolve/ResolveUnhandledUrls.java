package com.soundcloud.android.activity.resolve;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.main.WebViewActivity;

import android.net.Uri;

public class ResolveUnhandledUrls extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.JOBS_PAGE;
    }

    public void testResolveUrlShouldOpenInWebView() throws Exception {
        waiter.waitForActivity(WebViewActivity.class);
        assertThat(solo.getCurrentActivity().getClass().getSimpleName(), is(WebViewActivity.class.getSimpleName()));
    }
}
