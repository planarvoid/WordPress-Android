package com.soundcloud.android.tests.stream;

import static com.soundcloud.android.framework.matcher.screen.IsVisible.visible;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.framework.annotation.EventTrackingTest;
import com.soundcloud.android.framework.helpers.ConfigurationHelper;
import com.soundcloud.android.framework.helpers.mrlogga.TrackingActivityTest;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;

public class StreamUpsellTest extends TrackingActivityTest<MainActivity> {

    private static final String STREAM_UPSELL_TRACKING_TEST = "stream_upsell_tracking_test";

    public StreamUpsellTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.upsellUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    protected void beforeStartActivity() {
        ConfigurationHelper.enableUpsell(getInstrumentation().getTargetContext());
    }

    public void testUserCanNavigateToSubscribePageFromPlayer() {
        VisualPlayerElement player = mainNavHelper
                .goToStream()
                .scrollToFirstSnippedTrack()
                .clickToPlay();

        assertThat(player.clickUpgrade(), is(visible()));
    }

    @EventTrackingTest
    public void testUserCanNavigateToSubscribePageFromUpsell() {
        final StreamScreen streamScreen = mainNavHelper.goToStream();

        startEventTracking();

        assertThat(streamScreen
                .scrollToUpsell()
                .clickUpgrade(), is(visible()));

        finishEventTracking(STREAM_UPSELL_TRACKING_TEST);
    }
}
