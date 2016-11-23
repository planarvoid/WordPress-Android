package com.soundcloud.android.tests.player;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.screens.TrackInfoScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.TestConsts;
import com.soundcloud.android.tests.activity.resolve.ResolveBaseTest;

import android.net.Uri;

public class TrackWithNoDescriptionTest extends ResolveBaseTest {

    @Override
    protected void logInHelper() {
        TestUser.playerUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected Uri getUri() {
        return TestConsts.TRACK_WITH_NO_DESCRIPTION;
    }

    public void testPlayerShowTheTrackNoDescription() {
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(solo);
        assertTrue(visualPlayerElement.isVisible());

        String originalTitle = visualPlayerElement.getTrackTitle();
        TrackInfoScreen trackInfoScreen = visualPlayerElement
                .clickMenu()
                .clickInfo();

        assertThat(originalTitle, is(equalTo(trackInfoScreen.getTitle())));
        assertTrue(trackInfoScreen.getNoDescription().isOnScreen());
    }
}
