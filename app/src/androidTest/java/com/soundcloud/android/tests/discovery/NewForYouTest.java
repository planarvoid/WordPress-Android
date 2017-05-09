package com.soundcloud.android.tests.discovery;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.FeatureFlagsHelper;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.discovery.NewForYouScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.tests.ActivityTest;

public class NewForYouTest extends ActivityTest<MainActivity> {
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

    @Override
    protected void beforeStartActivity() {
        FeatureFlagsHelper.create(getInstrumentation().getTargetContext()).enable(Flag.NEW_FOR_YOU_FIRST);
    }

    @Override
    protected void tearDown() throws Exception {
        FeatureFlagsHelper.create(getInstrumentation().getTargetContext()).reset(Flag.NEW_FOR_YOU_FIRST);
        super.tearDown();
    }

    public void testNewForYouPlayback() throws Exception {
        mrLocalLocal.startEventTracking();

        final NewForYouScreen newForYouScreen = discoveryScreen.newForYouBucket()
                                                               .clickViewAll();

        assertThat(newForYouScreen, is(visible()));
        assertThat("New for you screen title should be 'The Upload'",
                   newForYouScreen.getActionBarTitle(),
                   equalTo("The Upload"));

        final VisualPlayerElement player = newForYouScreen.clickHeaderPlay();

        assertTrue(player.isExpanded());

        mrLocalLocal.verify("specs/new_for_you_playback.spec");
    }

    // Note: I had to split this test into 2 to reduce flakiness caused by `player:max / player:min` click events.
    public void testNewForYouEngagement() throws Exception {
        mrLocalLocal.startEventTracking();

        final NewForYouScreen newForYouScreen = discoveryScreen.newForYouBucket()
                                                               .clickViewAll();

        assertThat(newForYouScreen, is(visible()));
        assertThat("New for you screen title should be 'The Upload'",
                   newForYouScreen.getActionBarTitle(),
                   equalTo("The Upload"));

        newForYouScreen.toggleTrackLike(0);

        mrLocalLocal.verify("specs/new_for_you_engagement3.spec");
    }
}
