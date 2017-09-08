package com.soundcloud.android.tests.player;

import static com.soundcloud.android.framework.TestUser.playerUser;
import static com.soundcloud.android.tests.TestConsts.TRACK_WITH_DESCRIPTION;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.screens.TrackInfoScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.TestConsts;
import com.soundcloud.android.tests.activity.resolve.ResolveBaseTest;
import org.junit.Test;

import android.net.Uri;

public class TrackWithDescriptionTest extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TRACK_WITH_DESCRIPTION;
    }

    @Override
    protected TestUser getUserForLogin() {
        return playerUser;
    }

    @Test
    public void testShowDescription() throws Exception {
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(solo);
        assertTrue(visualPlayerElement.isVisible());

        String originalTitle = visualPlayerElement.getTrackTitle();
        TrackInfoScreen trackInfoScreen = visualPlayerElement
                .clickMenu()
                .clickInfo();

        assertThat(originalTitle, is(equalTo(trackInfoScreen.getTitle())));
        assertTrue(trackInfoScreen.getDescription().isOnScreen());
    }
}
