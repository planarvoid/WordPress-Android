package com.soundcloud.android.framework.helpers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.Waiter;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class PlayerHelper {

    public static VisualPlayerElement playPublicTrack(ActivityTest activityTest, MainNavigationHelper mainNavHelper) {
        final Waiter waiter = activityTest.getWaiter();
        waiter.waitForContentAndRetryIfLoadingFailed();
        VisualPlayerElement playerElement = mainNavHelper.goToDiscovery().trackRecommendationsBucket().clickFirstRecommendedTrack();
        playerElement.waitForExpandedPlayer();
        playerElement.waitForContent();
        return playerElement;
    }

    public static void assertSwipeToNextTrack(VisualPlayerElement playerElement) {
        final String startingTrack = playerElement.getTrackTitle();
        playerElement.swipeNext();

        assertThat("Failed to swipe to next track", isNotCurrentTrack(playerElement, startingTrack), is(true));
    }

    public static boolean isNotCurrentTrack(VisualPlayerElement playerElement, String title) {
        final ViewElement titleElement = playerElement.getTrackTitleViewElement();
        final boolean titleNotVisible = !titleElement.hasVisibility();
        return titleNotVisible || !new TextElement(titleElement).getText().equals(title);
    }
}
