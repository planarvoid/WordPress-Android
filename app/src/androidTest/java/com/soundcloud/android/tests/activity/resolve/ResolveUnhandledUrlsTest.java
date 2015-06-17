package com.soundcloud.android.tests.activity.resolve;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.WebViewActivity;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class ResolveUnhandledUrlsTest extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.JOBS_PAGE;
    }

    public void testResolveUrlShouldOpenInWebView() {
        waiter.waitForActivity(WebViewActivity.class);
        assertThat(solo.getCurrentActivity().getClass().getSimpleName(), is(WebViewActivity.class.getSimpleName()));
    }
}
