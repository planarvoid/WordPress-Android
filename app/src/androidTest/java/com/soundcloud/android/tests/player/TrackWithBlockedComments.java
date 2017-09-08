package com.soundcloud.android.tests.player;

import static com.soundcloud.android.framework.TestUser.playerUser;
import static com.soundcloud.android.framework.matcher.view.IsVisible.visible;
import static com.soundcloud.android.tests.TestConsts.TRACK_BLOCKED_COMMENTS;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.TestConsts;
import com.soundcloud.android.tests.activity.resolve.ResolveBaseTest;
import org.junit.Test;

import android.net.Uri;

public class TrackWithBlockedComments extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TRACK_BLOCKED_COMMENTS;
    }

    @Override
    protected TestUser getUserForLogin() {
        return playerUser;
    }

    @Test
    public void testShowDescription() throws Exception {
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(solo);
        assertTrue(visualPlayerElement.isVisible());
        assertThat(visualPlayerElement.clickMenu().commentItem(), is(not(visible())));
    }
}
