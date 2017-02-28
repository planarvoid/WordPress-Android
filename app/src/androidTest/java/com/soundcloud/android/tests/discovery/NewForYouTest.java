package com.soundcloud.android.tests.discovery;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.discovery.NewForYouScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class NewForYouTest extends TrackingActivityTest<MainActivity> {
    private DiscoveryScreen discoveryScreen;

    public NewForYouTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return TestUser.defaultUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        discoveryScreen = mainNavHelper.goToDiscovery();
    }

    public void testNewForYouPlayback() {
        startEventTracking();

        final NewForYouScreen newForYouScreen = discoveryScreen.newForYouBucket()
                                                               .clickViewAll();

        assertThat(newForYouScreen, is(visible()));

        final VisualPlayerElement player = newForYouScreen.clickHeaderPlay();

        assertTrue(player.isExpanded());

        finishEventTracking("new_for_you_playback");
    }

    // Note: I had to split this test into 2 to reduce flakiness caused by `player:max / player:min` click events.
    public void testNewForYouEngagement() {
        startEventTracking();

        final NewForYouScreen newForYouScreen = discoveryScreen.newForYouBucket()
                                                               .clickViewAll();

        assertThat(newForYouScreen, is(visible()));

        newForYouScreen.toggleTrackLike(0);

        finishEventTracking("new_for_you_engagement3");
    }
}
