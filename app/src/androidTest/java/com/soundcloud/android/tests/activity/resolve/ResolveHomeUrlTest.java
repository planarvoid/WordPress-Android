package com.soundcloud.android.tests.activity.resolve;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static com.soundcloud.android.tests.TestConsts.HOME_URI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.tests.TestConsts;
import org.junit.Test;

import android.net.Uri;

public class ResolveHomeUrlTest extends ResolveBaseTest {
    @Override
    protected Uri getUri() {
        return HOME_URI;
    }

    @Test
    public void testShouldOpenStream() throws Exception {
        assertThat(new StreamScreen(solo), is(visible()));
    }
}
