package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.MainScreen;

import android.net.Uri;

public class ResolveBrokenLinkTest extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.BROKEN_LINK;
    }

    public void ignoretestShouldResolveBrokenLinks() {
        assertTrue("Stream should be visible", new MainScreen(solo).isVisible());
    }
}
