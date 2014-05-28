package com.soundcloud.android.activity.resolve;

import static com.soundcloud.android.tests.hamcrest.IsVisible.Visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.MainScreen;

import android.net.Uri;

public class ResolveBrokenLinkTest extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.BROKEN_LINK;
    }

    public void ignoretestShouldResolveBrokenLinks() {
        assertThat(new MainScreen(solo), is(Visible()));
    }
    
    public void testShouldResolveBrokenLinks() {
        solo.assertText("There was a problem loading that url");
    }
}
