package com.soundcloud.android.tests.player;

import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.TestConsts;
import com.soundcloud.android.tests.activity.resolve.ResolveBaseTest;

import android.net.Uri;

public class TrackWithBlockedComments extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TestConsts.TRACK_BLOCKED_COMMENTS;
    }

    @Override
    protected void logInHelper() {
        TestUser.playerUser.logIn(getInstrumentation().getTargetContext());
    }

    public void testShowDescription() {
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(solo);
        assertTrue(visualPlayerElement.isVisible());
        assertThat(visualPlayerElement.clickMenu().commentItem(), is(not(visible())));
    }
}
