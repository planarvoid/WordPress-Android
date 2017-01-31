package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.R;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.TestConsts;

import android.net.Uri;

public class ResolveBrokenLinkTest extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.BROKEN_LINK;
    }

    public void testShouldLandOnStreamIfCannotResolveDeeplink() {
        assertTrue(waiter.expectToastWithText(toastObserver, resourceString(R.string.error_loading_url)));
        assertThat(new StreamScreen(solo), is(visible()));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }
}
