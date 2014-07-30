package com.soundcloud.android.tests.helpers;

import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.Waiter;

public class PlayerHelper {

    public static VisualPlayerElement openPlayer(ActivityTestCase activityTestCase, ExploreScreen exploreScreen) {
        final Waiter waiter = activityTestCase.getWaiter();
        waiter.waitForContentAndRetryIfLoadingFailed();
        exploreScreen.playFirstTrack();
        VisualPlayerElement playerElement = new VisualPlayerElement(activityTestCase.getSolo());
        waiter.waitForExpandedPlayer();
        playerElement.waitForContent();
        return playerElement;
    }

    public static void skipToAd(VisualPlayerElement playerElement) {
        playerElement.swipeNext();
    }

}
