package com.soundcloud.android.tests.player;

import static com.soundcloud.android.framework.TestUser.playerUser;
import static com.soundcloud.android.tests.TestConsts.TRACK_LOTS_OF_COMMENTS;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.screens.AddCommentScreen;
import com.soundcloud.android.screens.TrackCommentsScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.TestConsts;
import com.soundcloud.android.tests.activity.resolve.ResolveBaseTest;
import org.junit.Test;

import android.net.Uri;

public class TrackCommentTest extends ResolveBaseTest {

    @Override
    protected Uri getUri() {
        return TRACK_LOTS_OF_COMMENTS;
    }

    @Override
    protected TestUser getUserForLogin() {
        return playerUser;
    }

    @Test
    public void testShowDescription() throws Exception {
        VisualPlayerElement visualPlayerElement = new VisualPlayerElement(solo);
        assertTrue(visualPlayerElement.isVisible());

        assertMakeComment(visualPlayerElement);
        assertCommentsPage(visualPlayerElement);
    }

    private void assertCommentsPage(VisualPlayerElement visualPlayerElement) {
        String originalTitle = visualPlayerElement.getTrackTitle();
        TrackCommentsScreen trackCommentsScreen = visualPlayerElement
                .clickMenu()
                .clickInfo()
                .clickComments();

        assertThat(originalTitle, is(equalTo(trackCommentsScreen.getTitle())));

        int initialCommentsCount = trackCommentsScreen.getCommentsCount();
        trackCommentsScreen.scrollToBottomOfComments();

        int nextCommentsCount = trackCommentsScreen.getCommentsCount();
        assertThat(nextCommentsCount, is(greaterThan(initialCommentsCount)));

        trackCommentsScreen.goBackToActivitiesScreen();
    }

    private void assertMakeComment(VisualPlayerElement visualPlayerElement) {
        final AddCommentScreen addCommentScreen = visualPlayerElement
                .clickMenu()
                .clickComment();

        assertTrue(addCommentScreen.waitForDialog());
        solo.goBack();
    }
}
