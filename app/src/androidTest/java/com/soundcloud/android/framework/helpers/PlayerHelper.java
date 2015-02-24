package com.soundcloud.android.framework.helpers;

import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.framework.Waiter;

public class PlayerHelper {

    public static VisualPlayerElement openPlayer(ActivityTest activityTest, ExploreScreen exploreScreen) {
        final Waiter waiter = activityTest.getWaiter();
        waiter.waitForContentAndRetryIfLoadingFailed();
        exploreScreen.playFirstTrack();
        VisualPlayerElement playerElement = new VisualPlayerElement(activityTest.getSolo());
        playerElement.waitForExpandedPlayer();
        playerElement.waitForContent();
        return playerElement;
    }

}
