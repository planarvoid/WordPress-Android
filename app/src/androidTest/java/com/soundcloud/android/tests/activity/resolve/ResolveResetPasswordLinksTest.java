package com.soundcloud.android.tests.activity.resolve;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.main.WebViewActivity;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class ResolveResetPasswordLinksTest extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.RESET_PASSWORD_LINK_WITH_TRACKING;
    }

    public void testResolveResetPasswordUrlShouldOpenInWebView() {
        waiter.waitForActivity(WebViewActivity.class);
        waiter.waitFiveSeconds();
        assertThat(solo.getCurrentActivity().getClass().getSimpleName(), is(WebViewActivity.class.getSimpleName()));
    }
}
