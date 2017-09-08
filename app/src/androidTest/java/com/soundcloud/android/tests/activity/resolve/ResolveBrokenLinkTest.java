package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.R.string;
import static com.soundcloud.android.R.string.error_unknown_navigation;
import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.tests.TestConsts.BROKEN_LINK;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.R;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.TestConsts;
import org.junit.Test;

import android.net.Uri;

public class ResolveBrokenLinkTest extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return BROKEN_LINK;
    }

    @Test
    public void testShouldLandOnStreamIfCannotResolveDeeplink() throws Exception {
        assertTrue(waiter.expectToastWithText(toastObserver, resourceString(error_unknown_navigation)));
        assertThat(new StreamScreen(solo), is(visible()));
    }

    @Override
    protected void observeToastsHelper() {
        toastObserver.observe();
    }
}
