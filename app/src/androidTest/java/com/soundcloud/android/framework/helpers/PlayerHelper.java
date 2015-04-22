package com.soundcloud.android.framework.helpers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;

import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.screens.explore.ExploreScreen;
import com.soundcloud.android.tests.ActivityTest;

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

    public static void assertSwipeToNextTrack(VisualPlayerElement visualPlayerElement) {
        final String startingTrack = visualPlayerElement.getTrackTitle();
        visualPlayerElement.swipeNext();
        assertThat("Failed to swipe to next track",
                startingTrack, is(not(equalTo(visualPlayerElement.getTrackTitle()))));
    }
}
