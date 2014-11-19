package com.soundcloud.android.activity.resolve;

import static com.soundcloud.android.tests.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.StreamScreen;

import android.net.Uri;

public class ResolveBrokenLinkTest extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.BROKEN_LINK;
    }

    public void testShouldLandOnStreamIfCannotResolveDeeplink() {
        waiter.expectToast().toHaveText("There was a problem loading that url");
        assertThat(new StreamScreen(solo), is(visible()));
    }
}
