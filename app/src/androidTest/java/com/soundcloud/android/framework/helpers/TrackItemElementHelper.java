package com.soundcloud.android.framework.helpers;


import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.screens.elements.TrackItemElement;
import com.soundcloud.android.tests.ActivityTest;

public class TrackItemElementHelper {

    public static boolean isLiked(ActivityTest activityTest, TrackItemElement trackItemElement) {
        boolean result = trackItemElement.clickOverflowButton().isLiked();
        activityTest.getSolo().goBack();
        return result;
    }

    public static void assertLikeActionOnUnlikedTrack(ActivityTest activityTest, TrackItemElement trackItemElement) {
        assertFalse("Unable to like track; track is already liked.", isLiked(activityTest, trackItemElement));
        trackItemElement.clickOverflowButton().toggleLike();
        assertTrue("Track is unliked. Should be liked.", isLiked(activityTest, trackItemElement));
    }
}
