package com.soundcloud.android.activity.resolve;

import static com.soundcloud.android.tests.hamcrest.IsVisible.Visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.TestConsts;
import com.soundcloud.android.screens.StreamScreen;

import android.net.Uri;

public class ResolveBrokenLinkTest extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.BROKEN_LINK;
    }

    public void test_should_land_on_stream_if_cannot_resolve_deeplink() {
        assertThat(new StreamScreen(solo), is(Visible()));
        assertThat(solo.getToast().getText(), is(equalToIgnoringCase("There was a problem loading that url")));
    }
}
