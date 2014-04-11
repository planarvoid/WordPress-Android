package com.soundcloud.android.activity.resolve;

import com.soundcloud.android.TestConsts;

import android.net.Uri;

public class ResolveBrokenLinkTest extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.BROKEN_LINK;
    }

    public void testShouldResolveBrokenLinks() {
        solo.assertText("There was a problem loading that url");
    }
}
